package com.noh.bigchaindb;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.net.ssl.HttpsURLConnection;

import com.bigchaindb.api.OutputsApi;
import com.bigchaindb.api.TransactionsApi;
import com.bigchaindb.builders.BigchainDbConfigBuilder;
import com.bigchaindb.builders.BigchainDbTransactionBuilder;
import com.bigchaindb.constants.Operations;
import com.bigchaindb.model.BigChainDBGlobals;
import com.bigchaindb.model.Connection;
import com.bigchaindb.model.FulFill;
import com.bigchaindb.model.MetaData;
import com.bigchaindb.model.Outputs;
import com.bigchaindb.model.Transaction;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.i2p.crypto.eddsa.EdDSAPrivateKey;
import net.i2p.crypto.eddsa.EdDSAPublicKey;
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable;
import net.i2p.crypto.eddsa.spec.EdDSAParameterSpec;
import net.i2p.crypto.eddsa.spec.EdDSAPrivateKeySpec;
import net.i2p.crypto.eddsa.spec.EdDSAPublicKeySpec;

public class BigchainDBDriver {

	public static String publicKeyObjToStrInBase58(EdDSAPublicKey publicKey) {
		return Base58Utils.encode(Arrays.copyOfRange(publicKey.getEncoded(), 12, 44));
	}

	public static String privateKeyObjToStrInBase58(EdDSAPrivateKey key) {
		return Base58Utils.encode(Arrays.copyOfRange(key.getEncoded(), 16, 48));
	}

	public static EdDSAPublicKey publicKeyStrInBase58ToObj(String publicKeyStr) {
		byte[] pubkeyByte = Base58Utils.decode(publicKeyStr);
		EdDSAParameterSpec keySpecs = EdDSANamedCurveTable.getByName("Ed25519");
		EdDSAPublicKeySpec publicSpecs = new EdDSAPublicKeySpec(pubkeyByte, keySpecs);
		EdDSAPublicKey publicKeyObj = new EdDSAPublicKey(publicSpecs);
		return publicKeyObj;
	}

	public static EdDSAPrivateKey privateKeyStrInBase58ToObj(String privateKeyStr) {
		byte[] privateKeyByte = Base58Utils.decode(privateKeyStr);
		byte[] seed = Arrays.copyOfRange(privateKeyByte, 0, privateKeyByte.length);
		EdDSAParameterSpec keySpecs = EdDSANamedCurveTable.getByName("Ed25519");
		EdDSAPrivateKeySpec privKeySpec = new EdDSAPrivateKeySpec(seed, keySpecs);
		EdDSAPrivateKey privateKeyObj = new EdDSAPrivateKey(privKeySpec);
		return privateKeyObj;
	}

	public static String createAssetStr(String RESTAPI_SERVER, Map<String, String> assetData, MetaData metaData,
			String amount, EdDSAPublicKey creatorPublicKey, EdDSAPrivateKey creatorPrivateKey) {
		try {
			Transaction createTransaction = BigchainDbTransactionBuilder.init().addAssets(assetData, TreeMap.class)
					.addMetaData(metaData).operation(Operations.CREATE).addOutput(amount, creatorPublicKey)
					.buildAndSignOnly(creatorPublicKey, creatorPrivateKey);
			System.out.println("Created Transaction ID : " + createTransaction.getId());
			System.out.println("Created Transaction Details :" + createTransaction.toString());
			BigchainDBDriver.commitTx(RESTAPI_SERVER, createTransaction.toString());
			return createTransaction.toString();
		} catch (Exception e) {
			System.err.println(e.getMessage());
			return null;
		}
	}



	public static String transferAssetStr(String RESTAPI_SERVER, String txId, String outputIndex, String sendingAmount,
			MetaData metadata, EdDSAPublicKey senderPublicKey, EdDSAPrivateKey senderPrivateKey,
			EdDSAPublicKey receiverPublicKey) {

		String txStr = BigchainDBDriver.getTransactionRest(RESTAPI_SERVER, txId);

		// convert transaction data type from String to JsonObject
		JsonParser sendingTxParser = new JsonParser();
		JsonObject sendingTxJson = sendingTxParser.parse(txStr).getAsJsonObject();

		// get ready for Transfer Transaction Informations
		Map<String, String> transferInfo = new TreeMap<String, String>();
		transferInfo.put("txId", txId);
		transferInfo.put("outputIndex", outputIndex);
		transferInfo.put("senderBeforeAmount", sendingTxJson.get("transaction").getAsJsonObject().get("outputs")
				.getAsJsonArray().get(Integer.parseInt(outputIndex)).getAsJsonObject().get("amount").getAsString());
		transferInfo.put("sendAmount", sendingAmount);

		// getting an assetId
		if (sendingTxJson.get("transaction").getAsJsonObject().get("operation").getAsString().indexOf("CREATE") != -1) {
			transferInfo.put("assetId", sendingTxJson.get("transactionId").getAsString());
		} else {
			transferInfo.put("assetId", sendingTxJson.get("transaction").getAsJsonObject().get("asset")
					.getAsJsonObject().get("id").getAsString());
		}

		metadata.setMetaData("type", "assetTransfer");

		final FulFill fullFill = new FulFill();
		fullFill.setTransactionId(transferInfo.get("txId"));
		fullFill.setOutputIndex(Integer.parseInt(transferInfo.get("outputIndex")));
		String assetId = transferInfo.get("assetId");
		String receiverAmount = transferInfo.get("sendAmount");

		try {
			String senderBeforeAmount = transferInfo.get("senderBeforeAmount");
			String senderAfterAmount = Integer
					.toString(Integer.parseInt(senderBeforeAmount) - Integer.parseInt(receiverAmount));

			Transaction transferTransaction = null;
			System.out.println("senderAfterAmount:" + senderAfterAmount);
			if (Integer.parseInt(senderAfterAmount) == 0) {
				// Use the previous transaction's asset and TRANSFER it
				transferTransaction = BigchainDbTransactionBuilder.init().addMetaData(metadata)
						.addInput(null, fullFill, senderPublicKey).addOutput(receiverAmount, receiverPublicKey)
						.addAssets(assetId, String.class).operation(Operations.TRANSFER)
						.buildAndSignOnly(senderPublicKey, senderPrivateKey);

			} else if (Integer.parseInt(senderAfterAmount) > 0) {
				// Use the previous transaction's asset and TRANSFER it
				transferTransaction = BigchainDbTransactionBuilder.init().addMetaData(metadata)
						.addInput(null, fullFill, senderPublicKey).addOutput(receiverAmount, receiverPublicKey)
						.addOutput(senderAfterAmount, senderPublicKey).addAssets(assetId, String.class)
						.operation(Operations.TRANSFER).buildAndSignOnly(senderPublicKey, senderPrivateKey);
			} else {
				System.err.println("sender has insufficient amount of asset");
				System.err.println("sender has :" + senderBeforeAmount);
				System.err.println("sending amount :" + receiverAmount);
				return null;
			}
			System.out.println("transferTransaction.toString():" + transferTransaction.toString());

			try {
				if (transferTransaction.toString() != null) {
					// request of transfer Asset to RESTAPI Server
					BigchainDBDriver.commitTx(RESTAPI_SERVER, transferTransaction.toString());
					System.out.println("An Asset Str Transfer has been Succeeded!!");
				} else {
					System.out.println("An Asset Transfer has been Failed!!");
				}
			} catch (Exception e) {
				System.out.println("ERROR:" + e.getMessage());
			}

			return transferTransaction.toString();
		} catch (Exception e) {
			System.err.println("ERROR:" + e.getMessage());
			return null;
		}
	}

	public static ArrayList<String> sendCoinStr(String RESTAPI_SERVER, EdDSAPublicKey senderPublicKey,
			EdDSAPrivateKey senderPrivateKey, EdDSAPublicKey receiverPublicKey, String sendingAmountStr,
			MetaData metaData) {
		String senderPublicKeyStr = BigchainDBDriver.publicKeyObjToStrInBase58(senderPublicKey);
		int sentAmount = 0;
		ArrayList<String> transferredTxIds = null;

		int balance = getSumCoinRest(RESTAPI_SERVER, senderPublicKeyStr);
		int sendingAmount = Integer.parseInt(sendingAmountStr);

		metaData.setMetaData("asset_type", "coin");

		if (balance >= sendingAmount) {
			transferredTxIds = new ArrayList<String>();
			try {
				JsonArray outputs = BigchainDBDriver.getUnspentOutputsRest(RESTAPI_SERVER, senderPublicKeyStr);
				if (outputs != null) {
					int outputSize = outputs.size();

					System.out.println("output size:" + Integer.toString(outputSize));

					for (int i = 0; i < outputSize && sendingAmount > sentAmount; i++) {
						String txId = outputs.get(i).getAsJsonObject().get("transaction_id").getAsString();
						int outputIndex = outputs.get(i).getAsJsonObject().get("output_index").getAsInt();

						String txStr = getTransactionRest(RESTAPI_SERVER, txId);
						if (isCoin(txStr) == true) {
							JsonParser parser = new JsonParser();
							JsonObject tx = parser.parse(txStr).getAsJsonObject();
							int txAmount = tx.get("transaction").getAsJsonObject().get("outputs").getAsJsonArray()
									.get(outputIndex).getAsJsonObject().get("amount").getAsInt();

							System.out.println("txAmount:" + Integer.toString(txAmount));

							String amount = "";

							if ((sendingAmount - sentAmount) > txAmount) {
								amount = Integer.toString(txAmount);
							} else {
								amount = Integer.toString(sendingAmount - sentAmount);
							}

							transferAssetStr(RESTAPI_SERVER, txId, Integer.toString(outputIndex), amount, metaData,
									senderPublicKey, senderPrivateKey, receiverPublicKey);

							transferredTxIds.add(txId);
							sentAmount += Integer.parseInt(amount);
						}
					}
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			System.err.println("There is insufficient Coin");

		}

		return transferredTxIds;
	}

	private static String getPostDataString(HashMap<String, String> params) throws UnsupportedEncodingException {
		StringBuilder result = new StringBuilder();
		boolean first = true;
		for (Map.Entry<String, String> entry : params.entrySet()) {
			if (first)
				first = false;
			else
				result.append("&");

			result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
			result.append("=");
			result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
		}

		return result.toString();
	}

	public static String commitTx(String RESTAPI_SERVER, String transactionJson) {
		String response = "";
		String strUrl = RESTAPI_SERVER + "/transactions/commit";
		try {
			URL url = new URL(strUrl);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(5000); // 서버에 연결되는 Timeout 시간 설정
			conn.setReadTimeout(5000); // InputStream 읽어 오는 Timeout 시간 설정
			conn.setRequestMethod("POST");
			conn.setDoInput(true);
			conn.setDoOutput(true); // POST 데이터를 OutputStream으로 넘겨 주겠다는 설정

			HashMap<String, String> postDataParams = new HashMap();
			postDataParams.put("transactionJson", transactionJson);

			OutputStream os = conn.getOutputStream();
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
			writer.write(getPostDataString(postDataParams));

			writer.flush();
			writer.close();
			os.close();
			int responseCode = conn.getResponseCode();

			if (responseCode == HttpsURLConnection.HTTP_OK) {
				String line;
				BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
				while ((line = br.readLine()) != null) {
					response += line;
				}
			} else {
				response = "";

			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return response;
	}

	/*
	 * this function judges whether the Transaction is Coin or Not
	 */
	public static boolean isCoin(String metadataStr) {
		boolean isCoin = false;
		JsonParser parser = new JsonParser();
		JsonObject metadataJson = parser.parse(metadataStr).getAsJsonObject();
		try {
			if (metadataJson.get("transaction").getAsJsonObject().get("metadata").getAsJsonObject().get("asset_type")
					.getAsString().indexOf("coin") != -1) {
				isCoin = true;
			}
			return isCoin;
		} catch (Exception e) {
			System.err.println(e.getLocalizedMessage());
			return isCoin;
		}
	}

	public static int getSumCoinRest(String RESTAPI_SERVER, String publicKey) {
		int sumCoin = 0;
		String strUrl = RESTAPI_SERVER + "/coins/sum/" + publicKey;
		try {
			URL url = new URL(strUrl);
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.setConnectTimeout(5000); // 서버에 연결되는 Timeout 시간 설정
			con.setReadTimeout(5000); // InputStream 읽어 오는 Timeout 시간 설정
			con.setRequestMethod("GET");
			con.setDoOutput(false);

			StringBuilder sb = new StringBuilder();
			if (con.getResponseCode() == HttpURLConnection.HTTP_OK) {
				BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), "utf-8"));
				String line;
				while ((line = br.readLine()) != null) {
					sb.append(line).append("\n");
				}
				br.close();
				JsonParser parser = new JsonParser();
				JsonObject metadataJson = parser.parse(sb.toString()).getAsJsonObject();
				sumCoin = metadataJson.get("sum").getAsInt();
			} else {
				System.out.println(con.getResponseMessage());
			}

		} catch (Exception e) {
			System.err.println(e.toString());
		}

		return sumCoin;
	}

	public static String getTransactionRest(String RESTAPI_SERVER, String txId) {
		String txStr = "";
		String strUrl = RESTAPI_SERVER + "/transaction/view/" + txId;
		try {
			URL url = new URL(strUrl);
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.setConnectTimeout(5000); // 서버에 연결되는 Timeout 시간 설정
			con.setReadTimeout(5000); // InputStream 읽어 오는 Timeout 시간 설정
			con.setRequestMethod("GET");
			con.setDoOutput(false);

			StringBuilder sb = new StringBuilder();
			if (con.getResponseCode() == HttpURLConnection.HTTP_OK) {
				BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), "utf-8"));
				String line;
				while ((line = br.readLine()) != null) {
					sb.append(line).append("\n");
				}
				br.close();
				JsonParser parser = new JsonParser();
				JsonObject transactionJson = parser.parse(sb.toString()).getAsJsonObject();
				txStr = transactionJson.toString();
			} else {
				System.out.println(con.getResponseMessage());
			}

		} catch (Exception e) {
			System.err.println("ERROR in getTransactionRest:" + e.getMessage());
		}

		return txStr;
	}

	public static JsonArray getUnspentOutputsRest(String RESTAPI_SERVER, String publicKey) {
		JsonArray outputs = null;
		String strUrl = RESTAPI_SERVER + "/outputs/list/unspent/" + publicKey;
		try {
			URL url = new URL(strUrl);
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.setConnectTimeout(5000); // 서버에 연결되는 Timeout 시간 설정
			con.setReadTimeout(5000); // InputStream 읽어 오는 Timeout 시간 설정
			con.setRequestMethod("GET");
			con.setDoOutput(false);

			StringBuilder sb = new StringBuilder();
			if (con.getResponseCode() == HttpURLConnection.HTTP_OK) {
				BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), "utf-8"));
				String line;
				while ((line = br.readLine()) != null) {
					sb.append(line).append("\n");
				}
				br.close();
				JsonParser parser = new JsonParser();
				JsonObject transactionJson = parser.parse(sb.toString()).getAsJsonObject();
				if (transactionJson.has("outputs") == true) {
					outputs = transactionJson.get("outputs").getAsJsonArray();
				}
			} else {
				System.out.println(con.getResponseMessage());
			}

		} catch (Exception e) {
			System.err.println("ERROR in getUnspentOutputsRest:" + e.getMessage());
			return outputs;
		}

		return outputs;
	}

	public static KeyPair decodeKeyPair(byte[] encodedPrivateKey) {
		EdDSAParameterSpec keySpecs = EdDSANamedCurveTable.getByName("Ed25519");
		byte[] seed = Arrays.copyOfRange(encodedPrivateKey, 0, encodedPrivateKey.length);
		EdDSAPrivateKeySpec privKeySpec = new EdDSAPrivateKeySpec(seed, keySpecs);
		EdDSAPublicKeySpec pubKeySpec = new EdDSAPublicKeySpec(privKeySpec.getA(), keySpecs);
		return new KeyPair(new EdDSAPublicKey(pubKeySpec), new EdDSAPrivateKey(privKeySpec));
	}

	/**
	 * configures connection url and credentials
	 */
	public static void setConfig(String appId, String appKey, String[] baseUrls) {
		// define headers for connections
		Map<String, String> headers = new HashMap<String, String>();
		headers.put("app_id", appId);
		headers.put("app_key", appKey);

		List<Connection> connections = new ArrayList<Connection>();

		for (int i = 0; i < baseUrls.length; i++) {

			Map<String, Object> connConfig = new HashMap<String, Object>();
			// config connection
			connConfig.put("baseUrl", baseUrls[i]);
			connConfig.put("headers", headers);
			Connection conn = new Connection(connConfig);

			connections.add(conn);
		}

		BigchainDbConfigBuilder.addConnections(connections).setTimeout(60000) // override default timeout of 20000
																				// milliseconds
				.setup();
		BigChainDBGlobals.setBaseUrl(baseUrls[0] + "/api/v1");
		BigChainDBGlobals.setTimeout(60000);
		// BigchainDbConfigBuilder.baseUrl("http://192.168.197.7:9984/") // or use
		// http://testnet.bigchaindb.com
//	.addToken("app_id","").addToken("app_key","").setup();
	}

	/**
	 * generates EdDSA keypair to sign and verify transactions
	 * 
	 * @return KeyPair
	 */
	public static KeyPair getKeys() {
		// prepare your keys
		net.i2p.crypto.eddsa.KeyPairGenerator edDsaKpg = new net.i2p.crypto.eddsa.KeyPairGenerator();
		KeyPair keyPair = edDsaKpg.generateKeyPair();
		System.out.println("(*) Keys Generated..");

		return keyPair;

	}
}
