package org.apollo;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.apollo.common.crypto.SignInterface;
import org.apollo.common.crypto.SignUtils;
import org.apollo.common.parameter.CommonParameter;
import org.apollo.common.utils.ByteArray;
import org.apollo.common.utils.Sha256Hash;
import org.apollo.common.utils.StringUtil;
import org.apollo.core.config.DefaultConstants;
import org.apollo.core.config.args.Args;
import org.apollo.core.services.http.Util;

public class Test {

	public static void main(String[] args) {
//		// 衰减
//		float a = 0.6f;
//		// 父辈数量
//		int pn = 1;
//		int count = 100;
//		float n =1+a*a*a*a + a*a*a + a*a+ a;
//		System.out.println(n);
//		BigDecimal f1 = new BigDecimal(100).divide(new BigDecimal(n), 5);
//		System.out.println(f1);
//		BigDecimal f2 = f1.multiply(new BigDecimal(a));
//		System.out.println(f2);
//		BigDecimal f3 = f2.multiply(new BigDecimal(a));
//		System.out.println(f3);
//		BigDecimal f4 = f3.multiply(new BigDecimal(a));
//		System.out.println(f4);
//		BigDecimal f5 = f4.multiply(new BigDecimal(a));
//		System.out.println(f5);
//		System.out.println(f1.add(f2).add(f3).add(f4).add(f5));
////		X + (X * 0.7) + ((X * 0.7) * 0.7) + ((X * 0.7) * 0.7) * 0.7) + ((X * 0.7) * 0.7) * 0.7)*0.7 = 100
//		BigDecimal payAmount = new BigDecimal(count);
//		BigDecimal totalAmount = new BigDecimal(0);
//		float nn = 1;
//		  for(int i=1; i<pn;i++) {
//			  float n1 = IpcConstants.FAMILY_REWARD_DECAY_RATE;
//			  for(int j=1;j<i;j++){
//				  n1 = n1*IpcConstants.FAMILY_REWARD_DECAY_RATE;
//			  }
//			  nn += n1;
//		  }
//		  System.out.println(nn);
//		  BigDecimal currentAncestorsAmount = payAmount.divide(new BigDecimal(nn), pn);
//		  for(int j=pn-1; j>=0;j--){
//			  totalAmount = totalAmount.add(currentAncestorsAmount);
//			  System.out.println(currentAncestorsAmount.longValue());
//			  currentAncestorsAmount = currentAncestorsAmount.multiply(new BigDecimal(a));
//		  }
//		  System.out.println(totalAmount.longValue());
		String s2 = "ipc";
		System.out.println(s2);
		System.out.println(Util.getHexString(s2));
		byte[] address = createAddress(Util.getHexString(s2).getBytes());
		String base58check = StringUtil.encode58Check(address);
	    String hexString = ByteArray.toHexString(address);
	    System.out.println(base58check);
	    System.out.println(hexString);
		System.out.println(BigInteger.valueOf(10).pow(18));
		
	}
	
	public static byte[] createAddress(byte[] passPhrase) {
	    byte[] privateKey = pass2Key(passPhrase);
	    SignInterface ecKey = SignUtils.fromPrivate(privateKey,
	        Args.getInstance().isECKeyCryptoEngine());
	    return ecKey.getAddress();
	  }
	
	public static byte[] pass2Key(byte[] passPhrase) {
	    return Sha256Hash.hash(CommonParameter
	        .getInstance().isECKeyCryptoEngine(), passPhrase);
	  }

}
