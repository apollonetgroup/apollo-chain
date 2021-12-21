package org.apollo.core.service;

import com.google.protobuf.ByteString;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.collections4.CollectionUtils;
import org.apollo.common.utils.StringUtil;
import org.apollo.core.capsule.AccountCapsule;
import org.apollo.core.capsule.WitnessCapsule;
import org.apollo.core.config.DefaultConstants;
import org.apollo.core.config.Parameter.ChainConstant;
import org.apollo.core.exception.BalanceInsufficientException;
import org.apollo.core.store.AccountStore;
import org.apollo.core.store.DelegationStore;
import org.apollo.core.store.DynamicPropertiesStore;
import org.apollo.core.store.WitnessStore;
import org.apollo.protos.Protocol.Vote;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.stereotype.Component;

@Slf4j(topic = "mortgage")
@Component
public class MortgageService {

	@Setter
	private WitnessStore witnessStore;

	@Setter
	@Getter
	private DelegationStore delegationStore;

	@Setter
	private DynamicPropertiesStore dynamicPropertiesStore;

	@Setter
	private AccountStore accountStore;

	public void initStore(WitnessStore witnessStore, DelegationStore delegationStore, DynamicPropertiesStore dynamicPropertiesStore, AccountStore accountStore) {
		this.witnessStore = witnessStore;
		this.delegationStore = delegationStore;
		this.dynamicPropertiesStore = dynamicPropertiesStore;
		this.accountStore = accountStore;
	}

	public void payFounder() {
		AccountCapsule foundAccount = accountStore.getFounder();
		long founderApy = DefaultConstants.BLOCK_PRODUCE_FOUNDER_BONUS;
		adjustAllowance(foundAccount.getAddress().toByteArray(), founderApy);
	}

	public void payGrant() {
		AccountCapsule grantAccount = accountStore.getGrant();
		long founderApy = DefaultConstants.BLOCK_PRODUCE_VOTER_BONUS;
		long cycle = dynamicPropertiesStore.getCurrentCycleNumber();
		delegationStore.addReward(cycle, grantAccount.createDbKey(), founderApy);
	}

	public void payStandbyWitness() {
		List<WitnessCapsule> witnessCapsules = witnessStore.getAllWitnesses();
		Map<ByteString, WitnessCapsule> witnessCapsuleMap = new HashMap<>();
		List<ByteString> witnessAddressList = new ArrayList<>();
		for (WitnessCapsule witnessCapsule : witnessCapsules) {
			witnessAddressList.add(witnessCapsule.getAddress());
			witnessCapsuleMap.put(witnessCapsule.getAddress(), witnessCapsule);
		}
		witnessAddressList.sort(Comparator.comparingLong((ByteString b) -> witnessCapsuleMap.get(b).getVoteCount()).reversed()
				.thenComparing(Comparator.comparingInt(ByteString::hashCode).reversed()));
		if (witnessAddressList.size() > ChainConstant.WITNESS_STANDBY_LENGTH) {
			witnessAddressList = witnessAddressList.subList(0, ChainConstant.WITNESS_STANDBY_LENGTH);
		}
		long voteSum = 0;
		long totalPay = dynamicPropertiesStore.getWitness127PayPerBlock();
		for (ByteString b : witnessAddressList) {
			voteSum += witnessCapsuleMap.get(b).getVoteCount();
		}

		if (voteSum > 0) {
			for (ByteString b : witnessAddressList) {
				double eachVotePay = (double) totalPay / voteSum;
				long pay = (long) (witnessCapsuleMap.get(b).getVoteCount() * eachVotePay);
				logger.debug("pay {} stand reward {}", Hex.toHexString(b.toByteArray()), pay);
				payReward(b.toByteArray(), pay);
			}
		}
	}

	public void payBlockReward(byte[] witnessAddress, long value) {
		logger.debug("pay {} block reward {}", Hex.toHexString(witnessAddress), value);
		payReward(witnessAddress, value);
	}

	public void payTransactionFeeReward(byte[] witnessAddress, long value) {
		logger.debug("pay {} transaction fee reward {}", Hex.toHexString(witnessAddress), value);
		payReward(witnessAddress, value);
	}

	private void payReward(byte[] witnessAddress, long value) {
		long cycle = dynamicPropertiesStore.getCurrentCycleNumber();
		int brokerage = delegationStore.getBrokerage(cycle, witnessAddress);
		double brokerageRate = (double) brokerage / 100;
		long brokerageAmount = (long) (brokerageRate * value);
		value -= brokerageAmount;
		delegationStore.addReward(cycle, witnessAddress, value);
		adjustAllowance(witnessAddress, brokerageAmount);
	}

	private long payAncestors(AccountCapsule accountCapsule, long reward, boolean isConsulting) {
		List<ByteString> ancestors = accountCapsule.getAncestorsList();
		if (reward <= 0 || ancestors.isEmpty()) {
			return 0;
		}
		BigDecimal payAmount = new BigDecimal(reward * DefaultConstants.PAY_FOR_ANCESTORS_RATE);
		if (isConsulting) {
			return payAmount.longValue();
		}
		float n = 1;
		for (int i = 1; i < ancestors.size(); i++) {
			float n1 = DefaultConstants.FAMILY_REWARD_DECAY_RATE;
			for (int j = 1; j < i; j++) {
				n1 = n1 * DefaultConstants.FAMILY_REWARD_DECAY_RATE;
			}
			n += n1;
		}
		BigDecimal currentAncestorsAmount = payAmount.divide(new BigDecimal(n), ancestors.size());
		for (int j = ancestors.size() - 1; j >= 0; j--) {
			try {
				byte[] address = ancestors.get(j).toByteArray();
				adjustAllowance(address, currentAncestorsAmount.longValue());
				currentAncestorsAmount = currentAncestorsAmount.multiply(new BigDecimal(DefaultConstants.PAY_FOR_ANCESTORS_RATE));
			} catch (Throwable e) {
				logger.error("pay ancestors amount fail.", e);
			}
		}
		return payAmount.longValue();
	}

	public void withdrawReward(byte[] address) {
		if (!dynamicPropertiesStore.allowChangeDelegation()) {
			return;
		}
		AccountCapsule accountCapsule = accountStore.get(address);
		long beginCycle = delegationStore.getBeginCycle(address);
		long endCycle = delegationStore.getEndCycle(address);
		long currentCycle = dynamicPropertiesStore.getCurrentCycleNumber();
		long reward = 0;
		if (beginCycle > currentCycle || accountCapsule == null) {
			return;
		}
		if (beginCycle == currentCycle) {
			AccountCapsule account = delegationStore.getAccountVote(beginCycle, address);
			if (account != null) {
				return;
			}
		}
		// withdraw the latest cycle reward
		if (beginCycle + 1 == endCycle && beginCycle < currentCycle) {
			AccountCapsule account = delegationStore.getAccountVote(beginCycle, address);
			if (account != null) {
				reward = computeReward(beginCycle, endCycle, account);
				long pay = payAncestors(account, reward, false);
				reward -= pay;
				adjustAllowance(address, reward);
				reward = 0;
				logger.info("latest cycle reward {},{}", beginCycle, account.getVotesList());
			}
			beginCycle += 1;
		}
		//
		endCycle = currentCycle;
		if (CollectionUtils.isEmpty(accountCapsule.getVotesList())) {
			delegationStore.setBeginCycle(address, endCycle + 1);
			return;
		}
		if (beginCycle < endCycle) {
			reward += computeReward(beginCycle, endCycle, accountCapsule);
			long pay = payAncestors(accountCapsule, reward, false);
			reward -= pay;
			adjustAllowance(address, reward);
		}
		delegationStore.setBeginCycle(address, endCycle);
		delegationStore.setEndCycle(address, endCycle + 1);
		delegationStore.setAccountVote(endCycle, address, accountCapsule);
		logger.info("adjust {} allowance {}, now currentCycle {}, beginCycle {}, endCycle {}, " + "account vote {},", Hex.toHexString(address), reward, currentCycle, beginCycle,
				endCycle, accountCapsule.getVotesList());
	}

	public long queryReward(byte[] address) {
		if (!dynamicPropertiesStore.allowChangeDelegation()) {
			return 0;
		}
		AccountCapsule accountCapsule = accountStore.get(address);
		long beginCycle = delegationStore.getBeginCycle(address);
		long endCycle = delegationStore.getEndCycle(address);
		long currentCycle = dynamicPropertiesStore.getCurrentCycleNumber();
		long reward = 0;
		if (accountCapsule == null) {
			return 0;
		}
		if (beginCycle > currentCycle) {
			return accountCapsule.getAllowance();
		}
		// withdraw the latest cycle reward
		if (beginCycle + 1 == endCycle && beginCycle < currentCycle) {
			AccountCapsule account = delegationStore.getAccountVote(beginCycle, address);
			if (account != null) {
				reward = computeReward(beginCycle, endCycle, account);
				long pay = payAncestors(account, reward, true);
				reward -= pay;
			}
			beginCycle += 1;
		}
		//
		endCycle = currentCycle;
		if (CollectionUtils.isEmpty(accountCapsule.getVotesList())) {
			return reward + accountCapsule.getAllowance();
		}
		if (beginCycle < endCycle) {
			reward += computeReward(beginCycle, endCycle, accountCapsule);
			long pay = payAncestors(accountCapsule, reward, true);
			reward -= pay;
		}
		return reward + accountCapsule.getAllowance();
	}

	private long computeReward(long cycle, AccountCapsule accountCapsule) {
		long reward = 0;
		byte[] grsrAddress = accountStore.getGrant().createDbKey();
		long grantReward = delegationStore.getReward(cycle, grsrAddress);
		if (grantReward > 0) {
			long grantTotalVote = delegationStore.getWitnessVote(cycle, grsrAddress);
			long userVote = accountCapsule.getTronPower()/DefaultConstants.IPC_UNIT;
			double voteRate = (double) userVote / grantTotalVote;
			if(voteRate > 1) {
				voteRate = 1;
			}
			reward += voteRate * grantReward;
		}

		for (Vote vote : accountCapsule.getVotesList()) {
			byte[] srAddress = vote.getVoteAddress().toByteArray();
			long totalReward = delegationStore.getReward(cycle, srAddress);
			long totalVote = delegationStore.getWitnessVote(cycle, srAddress);
			if (totalVote == DelegationStore.REMARK || totalVote == 0) {
				continue;
			}
			long userVote = vote.getVoteCount();
			double voteRate = (double) userVote / totalVote;
			reward += voteRate * totalReward;
			logger.debug("computeReward {} {} {} {},{},{},{}", cycle, Hex.toHexString(accountCapsule.getAddress().toByteArray()), Hex.toHexString(srAddress), userVote, totalVote,
					totalReward, reward);
		}
		return reward;
	}

	/**
	 * Compute reward from begin cycle to end cycle, which endCycle must greater than beginCycle.
	 * While computing reward after new reward algorithm taking effective cycle number,
	 * it will use new algorithm instead of old way.
	 * 
	 * @param beginCycle begin cycle (include)
	 * @param endCycle end cycle (exclude)
	 * @param accountCapsule account capsule
	 * @return total reward
	 */
	private long computeReward(long beginCycle, long endCycle, AccountCapsule accountCapsule) {
		if (beginCycle >= endCycle) {
			return 0;
		}
		long reward = 0;
		long newAlgorithmCycle = dynamicPropertiesStore.getNewRewardAlgorithmEffectiveCycle();
		if (beginCycle < newAlgorithmCycle) {
			long oldEndCycle = Math.min(endCycle, newAlgorithmCycle);
			for (long cycle = beginCycle; cycle < oldEndCycle; cycle++) {
				reward += computeReward(cycle, accountCapsule);
			}
			beginCycle = oldEndCycle;
		}
		if (beginCycle < endCycle) {
			for (Vote vote : accountCapsule.getVotesList()) {
				byte[] srAddress = vote.getVoteAddress().toByteArray();
				BigInteger beginVi = delegationStore.getWitnessVi(beginCycle - 1, srAddress);
				BigInteger endVi = delegationStore.getWitnessVi(endCycle - 1, srAddress);
				BigInteger deltaVi = endVi.subtract(beginVi);
				if (deltaVi.signum() <= 0) {
					continue;
				}
				long userVote = vote.getVoteCount();
				reward += deltaVi.multiply(BigInteger.valueOf(userVote)).divide(DelegationStore.DECIMAL_OF_VI_REWARD).longValue();
			}
		}
		return reward;
	}

	public WitnessCapsule getWitnessByAddress(ByteString address) {
		return witnessStore.get(address.toByteArray());
	}

	public void adjustAllowance(byte[] address, long amount) {
		try {
			if (amount <= 0) {
				return;
			}
			adjustAllowance(accountStore, address, amount);
		} catch (BalanceInsufficientException e) {
			logger.error("withdrawReward error: {},{}", Hex.toHexString(address), address, e);
		}
	}

	public void adjustAllowance(AccountStore accountStore, byte[] accountAddress, long amount) throws BalanceInsufficientException {
		AccountCapsule account = accountStore.getUnchecked(accountAddress);
		long allowance = account.getAllowance();
		if (amount == 0) {
			return;
		}

		if (amount < 0 && allowance < -amount) {
			throw new BalanceInsufficientException(StringUtil.createReadableString(accountAddress) + " insufficient balance");
		}
		account.setAllowance(allowance + amount);
		accountStore.put(account.createDbKey(), account);
	}

	private void sortWitness(List<ByteString> list) {
		list.sort(Comparator.comparingLong((ByteString b) -> getWitnessByAddress(b).getVoteCount()).reversed()
				.thenComparing(Comparator.comparingInt(ByteString::hashCode).reversed()));
	}
}
