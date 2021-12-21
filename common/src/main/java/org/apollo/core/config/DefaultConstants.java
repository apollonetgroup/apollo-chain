package org.apollo.core.config;

import java.math.BigInteger;

public class DefaultConstants {
	public static final String FOUNDER_ADDR = "ipc";
	public static final String BLACK_HOLE_NAME = "blackhole";
	public static final String DELEGATION_GRANT_NAME = "grant";
	public static final float FAMILY_REWARD_DECAY_RATE = 0.6f;
	public static final float FOUNDER_BONUS_RATE = 0.05f;
	public static final float ONE_LEVEL_DELEGATE_BONUS_RATE = 0.03f;
	public static final float TWO_LEVEL_DELEGATE_BONUS_RATE = 0.02f;
	public static final float VOTER_BONUS_RATE =  0.9f;
	public static final float PAY_FOR_ANCESTORS_RATE = 0.06f;
	
	public static final int ONE_LEVEL_DELEGATE_NUM = 20;
	public static final int TWO_LEVEL_DELEGATE_NUM = 120;
	public static final int IPC_UNIT = 1000000;
	public static final BigInteger DECIMAL_OF_VI_REWARD = BigInteger.valueOf(10).pow(16);
	
	public static final long ACCOUNT_UPGRADE_COST = 200000L;
	public static final int BLOCK_PRODUCE_CYCLE_MILLISECOND = 10000;
	public static final int BLOCK_PRODUCE_BONUS = IPC_UNIT*10;
	public static final long BLOCK_PRODUCE_ONE_LEVEL_BONUS = (long)(BLOCK_PRODUCE_BONUS*ONE_LEVEL_DELEGATE_BONUS_RATE);
	public static final long BLOCK_PRODUCE_TWO_LEVEL_BONUS = (long)(BLOCK_PRODUCE_BONUS*TWO_LEVEL_DELEGATE_BONUS_RATE);
	public static final long BLOCK_PRODUCE_FOUNDER_BONUS = (long)(BLOCK_PRODUCE_BONUS*FOUNDER_BONUS_RATE);
	public static final long BLOCK_PRODUCE_VOTER_BONUS = (long)(BLOCK_PRODUCE_BONUS*VOTER_BONUS_RATE);

}
