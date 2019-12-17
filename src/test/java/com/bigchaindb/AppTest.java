package com.bigchaindb;

import java.security.KeyPair;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

import com.bigchaindb.model.MetaData;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.noh.bigchaindb.BigchainDBDriver;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.i2p.crypto.eddsa.EdDSAPrivateKey;
import net.i2p.crypto.eddsa.EdDSAPublicKey;

/**
 * Unit test for simple App.
 */
public class AppTest extends TestCase {

	String RESTAPI_SERVER = "http://localhost:3000";

	/**
	 * Create the test case
	 *
	 * @param testName name of the test case
	 */
	public AppTest(String testName) {
		super(testName);
	}

	/**
	 * @return the suite of tests being tested
	 */
	public static Test suite() {
		return new TestSuite(AppTest.class);
	}

	public void getSumCoinRESTAPITestApp() {
		String publicKey = "GdJZ7yQKJAWkWRUzzDrEdt87wjxxQRnkK3qsRsSKaeh";

		int sumCoin = BigchainDBDriver.getSumCoinRest(RESTAPI_SERVER, publicKey);
		System.out.println("sumCoin:" + Integer.toString(sumCoin));
	}

	public void getTransactionRESTAPITestApp() {
		String txId = "b883a2bdef5366c95eb5234d4632635fc595142a3941bdbcf1930dc41890c014";

		System.out.println(BigchainDBDriver.getTransactionRest(RESTAPI_SERVER, txId));
	}

	public void getUnspentOutputsTestApp() {
		String txId = "6BDaFsbGkJ4UL2qUs9hFUoHWxTE4r5rjkZ66TooPWGkL";
		JsonArray outputs = BigchainDBDriver.getUnspentOutputsRest(RESTAPI_SERVER, txId);
		if (outputs != null) {
			System.out.println(outputs.size());
			System.out.println(outputs.toString());
		}

	}

	/**
	 * Create KeyPair Test App
	 */
	public void createKeysTestApp() {
		System.out.println("############# generate keypair #################");
		KeyPair keys = BigchainDBDriver.getKeys();
		EdDSAPublicKey publicKey = (EdDSAPublicKey) keys.getPublic();
		EdDSAPrivateKey privateKey = (EdDSAPrivateKey) keys.getPrivate();

		// print keypair as string
		System.out.println(BigchainDBDriver.publicKeyObjToStrInBase58(publicKey));
		System.out.println(BigchainDBDriver.privateKeyObjToStrInBase58(privateKey));
	}

	/**
	 * Create Asset Test App
	 */
	public void createAssetStrTestApp() {
		/*********************************************************************
		 * 
		 * Create Transaction Examples
		 * 
		 ***********************************************************************/
		String creatorPublicKeyStr = "4iZRPsSdASooxMGsEnzEbu9GuJqA58bTL215RDfZusZu";
		String creatorPrivateKeyStr = "Hq8myQQTzUgu6NJ15b1pzopdkwL8ho3hq71p3C6qckhm";

		// create Sender's keypair using base58 key strings.
		EdDSAPublicKey senderPublicKey = BigchainDBDriver.publicKeyStrInBase58ToObj(creatorPublicKeyStr);
		EdDSAPrivateKey senderPrivateKey = BigchainDBDriver.privateKeyStrInBase58ToObj(creatorPrivateKeyStr);

		// print Sender's keyPair
		System.out.println(BigchainDBDriver.publicKeyObjToStrInBase58(senderPublicKey));
		System.out.println(BigchainDBDriver.privateKeyObjToStrInBase58(senderPrivateKey));

		// Receiver's publicKey string.
		String receiverPublicKeyStr = "4uNsTJY6grDeHuw3tZeKhoCgVvr1ic9g1YBbCvKcFAis";

		// convert publicKey from String to EdDSAPublicKey
		EdDSAPublicKey receiverPublicKey = BigchainDBDriver.publicKeyStrInBase58ToObj(receiverPublicKeyStr);

		// print receiver's publicKey
		System.out.println(BigchainDBDriver.publicKeyObjToStrInBase58(receiverPublicKey));

		System.out.println("############# Create An Asset #################");
		// Add Asset Properties
		Map<String, String> assetData = new TreeMap<String, String>();
		assetData.put("city", "\"Berlin,AAA DE\"");
		assetData.put("temperature", "22");
		assetData.put("datetime", "\"" + new Date().toString() + "\"");

		// create metadata
		MetaData createMetaData = new MetaData();
		// add metadata properties
		createMetaData.setMetaData("what", "\"My first BigchainDB transaction\"");

		// setting amount of asset
		String createAmount = "10";

		// generate createAssetTransaction String
		String createdAssetStr = BigchainDBDriver.createAssetStr(RESTAPI_SERVER, assetData, createMetaData, createAmount, senderPublicKey,
				senderPrivateKey);

		assertTrue(true);
	}

	/**
	 * Transfer Asset Test App
	 */
	public void TransferAssetTestApp() {
		/*********************************************************************
		 * 
		 * Transfer Transaction Examples
		 * 
		 ***********************************************************************/
		// strings of sender's keys
		String senderPublicKeyStr = "4iZRPsSdASooxMGsEnzEbu9GuJqA58bTL215RDfZusZu";
		String senderPrivateKeyStr = "Hq8myQQTzUgu6NJ15b1pzopdkwL8ho3hq71p3C6qckhm";
		// objects of sender's keys
		EdDSAPublicKey senderPublicKey = BigchainDBDriver.publicKeyStrInBase58ToObj(senderPublicKeyStr);
		EdDSAPrivateKey senderPrivateKey = BigchainDBDriver.privateKeyStrInBase58ToObj(senderPrivateKeyStr);

		// a string of receiver's publicKey
		String receiverPublicKeyStr = "4uNsTJY6grDeHuw3tZeKhoCgVvr1ic9g1YBbCvKcFAis";
		// a object of receiver's publicKey
		EdDSAPublicKey receiverPublicKey = BigchainDBDriver.publicKeyStrInBase58ToObj(receiverPublicKeyStr);

		try {
			// setting txId
			String txId = "1794c532020533bc3d56d79949417d595465ab88b16b21ad486638603f75d684";
			if (txId != null) {
				// setting outputIndex of asset for sending
				String outputIndex = "0";
				// setting amount of sending
				String sendingAmount = "5";
				MetaData metadata = new MetaData();
				metadata.setMetaData("type", "assetTransfer");
				BigchainDBDriver.transferAssetStr(RESTAPI_SERVER, txId, outputIndex, sendingAmount, metadata, 
						senderPublicKey, senderPrivateKey, receiverPublicKey);
			}
		} catch (Exception e) {
			System.err.println("ERROR:" + e.getMessage());
		}

		assertTrue(true);
	}

	/**
	 * Create & Transfer Asset Test App
	 */
	public void createAndTransferAssetTestApp() {
		/*********************************************************************
		 * 
		 * Create & Transfer Transaction Examples
		 * 
		 ***********************************************************************/
		// strings of sender's keys
		String senderPublicKeyStr = "4iZRPsSdASooxMGsEnzEbu9GuJqA58bTL215RDfZusZu";
		String senderPrivateKeyStr = "Hq8myQQTzUgu6NJ15b1pzopdkwL8ho3hq71p3C6qckhm";

		// objects of sender's keys
		EdDSAPublicKey senderPublicKey = BigchainDBDriver.publicKeyStrInBase58ToObj(senderPublicKeyStr);
		EdDSAPrivateKey senderPrivateKey = BigchainDBDriver.privateKeyStrInBase58ToObj(senderPrivateKeyStr);

		System.out.println("############# Create An Asset #################");

		// New asset
		Map<String, String> assetData = new TreeMap<String, String>();
		assetData.put("city", "\"Berrlin, DE\""); // A value must be one string.
		assetData.put("temperature", "22");
		assetData.put("datetime", "\"" + new Date().toString() + "\"");
		// New metadata
		MetaData createMetaData = new MetaData();
		createMetaData.setMetaData("what", "\"My first BigchainDB transaction\"");

		// amount of asset
		String createAmount = "10";

		// generate a createAssetTransaction string
		String createdAssetStr = BigchainDBDriver.createAssetStr(RESTAPI_SERVER, assetData, createMetaData, createAmount, senderPublicKey,
				senderPrivateKey);

		try {
			Thread.sleep(3000); // It will take a little time to commit transaction

			// take an create transaction Id
			JsonParser parser = new JsonParser();
			JsonObject createTxJson = parser.parse(createdAssetStr).getAsJsonObject();

			String txId = createTxJson.get("id").getAsString();
			System.out.println("############# Transfer An Asset #################");
			if (txId != null) {
				String outputIndex = "0";
				String receiverAmount = "5";
				// a string of receiver's publicKey
				String receiverPublicKeyStr = "4uNsTJY6grDeHuw3tZeKhoCgVvr1ic9g1YBbCvKcFAis";

				// a object of receiver's publicKey
				EdDSAPublicKey receiverPublicKey = BigchainDBDriver.publicKeyStrInBase58ToObj(receiverPublicKeyStr);

				// getting sender's transaction that to send
				String txStr = BigchainDBDriver.getTransactionRest(RESTAPI_SERVER, txId);

				// convert transaction data type from String to JsonObject
				JsonParser sendingTxParser = new JsonParser();
				JsonObject sendingTxJson = sendingTxParser.parse(txStr).getAsJsonObject();
				String sendingAmount = sendingTxJson.get("transaction").getAsJsonObject().get("outputs").getAsJsonArray()
						.get(Integer.parseInt(outputIndex)).getAsJsonObject().get("amount").getAsString();

				// get ready for metadata
				MetaData transferMetaData = new MetaData();
				transferMetaData.setMetaData("type", "\"assetTransfer\"");

				// generate transferTransaction string. and commit it
				BigchainDBDriver.transferAssetStr(RESTAPI_SERVER, txId, outputIndex, sendingAmount, transferMetaData, 
						senderPublicKey, senderPrivateKey, receiverPublicKey);
			} else {
				System.out.println("txId is not valid!!");
			}
		} catch (Exception e) {
			System.err.println("ERROR:" + e.getMessage());

		}

		assertTrue(true);
	}

	/**
	 * Create Coin Test App
	 */
	public void createCoinStrTestApp() {
		/*********************************************************************
		 * 
		 * Create & Transfer Coin Examples
		 * 
		 ***********************************************************************/
		System.out.println("############# decode keypair #################");
		// encode a keypair in base58
		String adminPublicKeyStr = "GdJZ7yQKJAWkWRUzzDrEdt87wjxxQRnkK3qsRsSKaeh";
		String adminPrivateKeyStr = "2BaWjNsoc76uP6rwV4CWcgCDR9nauiYWuyfMnDnV2hcw";

		// create admin's keypair using base58 key strings.
		EdDSAPublicKey adminPublicKey = BigchainDBDriver.publicKeyStrInBase58ToObj(adminPublicKeyStr);
		EdDSAPrivateKey adminPrivateKey = BigchainDBDriver.privateKeyStrInBase58ToObj(adminPrivateKeyStr);

		// print admin's keyPair
		System.out.println(BigchainDBDriver.publicKeyObjToStrInBase58(adminPublicKey));
		System.out.println(BigchainDBDriver.privateKeyObjToStrInBase58(adminPrivateKey));

		System.out.println("############# Create Coin #################");
		// New asset
		String createdAt = new Date().toString();
		Map<String, String> coinData = new TreeMap<String, String>();
		coinData.put("name", "MGB TEST Coin");
		coinData.put("createdAt", createdAt);

		// metadata
		MetaData coinCreateMetaData = new MetaData();
		/**********************************************
		 * Very Important!! asset_type = coin <- coin identifier
		 **********************************************/
		coinCreateMetaData.setMetaData("asset_type", "coin");
		coinCreateMetaData.setMetaData("createdAt", "\"" + createdAt + "\"");

		String coinCreateAmount = "1000";
		String coinCreatedStr = BigchainDBDriver.createAssetStr(RESTAPI_SERVER, coinData, coinCreateMetaData,  coinCreateAmount, adminPublicKey,
				adminPrivateKey);
		
		assertTrue(true);

	}

	/**
	 * buying Coin Test App
	 */
	public void buyingCoinStrTestApp() {
		/*********************************************************************
		 * 
		 * Buying Coin Examples
		 * 
		 ***********************************************************************/
		System.out.println("############# keypair settings#################");
		// encode a keypair in base58
		String adminPublicKeyStr = "GdJZ7yQKJAWkWRUzzDrEdt87wjxxQRnkK3qsRsSKaeh";
		String adminPrivateKeyStr = "2BaWjNsoc76uP6rwV4CWcgCDR9nauiYWuyfMnDnV2hcw";

		// create admin's keypair using base58 key strings.
		EdDSAPublicKey adminPublicKey = BigchainDBDriver.publicKeyStrInBase58ToObj(adminPublicKeyStr);
		EdDSAPrivateKey adminPrivateKey = BigchainDBDriver.privateKeyStrInBase58ToObj(adminPrivateKeyStr);

		// create Receiver's publicKey using base58 key string.
		String receiverPublicKeyStr = "Bnrkg8TGdetrGRPHL8jZNNPEdSW13etntAQa7jGz4WgP";
		EdDSAPublicKey receiverPublicKey = BigchainDBDriver.publicKeyStrInBase58ToObj(receiverPublicKeyStr);

		String sendAmount = "18";
		MetaData sendMetadata = new MetaData();
		sendMetadata.setMetaData("type", "\"coinTransfer\"");

		ArrayList<String> result = BigchainDBDriver.sendCoinStr(RESTAPI_SERVER, adminPublicKey, adminPrivateKey, receiverPublicKey,
				sendAmount, sendMetadata);

		if (result != null) {
			for (int i = 0; i < result.size(); i++) {
				System.out.println("Transferred Transaction Id #" + Integer.toString(i) + " : " + result.get(i));
			}
		}
		assertTrue(true);

	}

	/**
	 * transfer Coin Test App
	 */
	public void transferCoinTestApp() {
		/*********************************************************************
		 * 
		 * Transfer Coin Examples
		 * 
		 ***********************************************************************/
		System.out.println("############# keypair settings#################");
		// encode a keypair in base58
		String senderPublicKeyStr = "A5t1thhVNUeKV3bXQCFL4HVXyMyMWYEjp1S3WfXQqjNC";
		String senderPrivateKeyStr = "8Cm3PWyt7kQK6FQLXdXzCDNicQiepzNBSN8bGKwon77R";
		// create Sender's keypair using base58 key strings.
		EdDSAPublicKey senderPublicKey = BigchainDBDriver.publicKeyStrInBase58ToObj(senderPublicKeyStr);
		EdDSAPrivateKey senderPrivateKey = BigchainDBDriver.privateKeyStrInBase58ToObj(senderPrivateKeyStr);

		// create Receiver's publicKey using base58 key string.
		String receiverPublicKeyStr = "Bnrkg8TGdetrGRPHL8jZNNPEdSW13etntAQa7jGz4WgP";
		EdDSAPublicKey receiverPublicKey = BigchainDBDriver.publicKeyStrInBase58ToObj(receiverPublicKeyStr);

		String sendAmount = "18";
		MetaData sendMetadata = new MetaData();
		sendMetadata.setMetaData("type", "\"coinTransfer\"");

		ArrayList<String> result = BigchainDBDriver.sendCoinStr(RESTAPI_SERVER, senderPublicKey, senderPrivateKey, receiverPublicKey,
				sendAmount, sendMetadata);

		if (result != null) {
			for (int i = 0; i < result.size(); i++) {
				System.out.println("Transferred Transaction Id #" + Integer.toString(i) + " : " + result.get(i));
			}
		}
		assertTrue(true);

	}
}
