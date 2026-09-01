package ao.cmc.fincrestsdvm.data.api

import java.io.File
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

private const val CERT_RESOURCE_PATH = "/certs/digicert-global-g2-tls-rsa-sha256-2020-ca1.pem"
private const val CERT_FILE_PATH = "certs/digicert-global-g2-tls-rsa-sha256-2020-ca1.pem"

/**
 * reporteshml.cmc.ao / reportes.cmc.ao don't send their DigiCert intermediate
 * certificate during the TLS handshake, so the default JVM trust store
 * rejects them with "unable to find valid certification path" — the same
 * problem the web app works around with Node's NODE_EXTRA_CA_CERTS. This
 * trusts the JVM's default roots *and* that intermediate explicitly, mirroring
 * that fix. If the cert isn't bundled/available, falls back to the default
 * trust manager alone (SIRA calls will fail TLS verification until it is).
 */
fun buildSiraTrustManager(): X509TrustManager {
    val defaultTrustManager = systemTrustManager()
    val extraCert = loadExtraCaCertificate() ?: run {
        System.err.println(
            "WARNING: $CERT_RESOURCE_PATH not found on the classpath or at ./$CERT_FILE_PATH — " +
                "SIRA TLS calls will fail with \"unable to find valid certification path\" until it is bundled."
        )
        return defaultTrustManager
    }
    val extraTrustManager = trustManagerFor(extraCert)

    return object : X509TrustManager {
        override fun checkClientTrusted(chain: Array<out X509Certificate>, authType: String) {
            defaultTrustManager.checkClientTrusted(chain, authType)
        }

        override fun checkServerTrusted(chain: Array<out X509Certificate>, authType: String) {
            try {
                defaultTrustManager.checkServerTrusted(chain, authType)
            } catch (_: Exception) {
                extraTrustManager.checkServerTrusted(chain, authType)
            }
        }

        override fun getAcceptedIssuers(): Array<X509Certificate> =
            defaultTrustManager.acceptedIssuers + extraTrustManager.acceptedIssuers
    }
}

private fun systemTrustManager(): X509TrustManager {
    val factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
    factory.init(null as KeyStore?)
    return factory.trustManagers.filterIsInstance<X509TrustManager>().first()
}

private fun trustManagerFor(cert: X509Certificate): X509TrustManager {
    val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
    keyStore.load(null, null)
    keyStore.setCertificateEntry("sira-extra-ca", cert)
    val factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
    factory.init(keyStore)
    return factory.trustManagers.filterIsInstance<X509TrustManager>().first()
}

private fun loadExtraCaCertificate(): X509Certificate? {
    val bytes = object {}.javaClass.getResourceAsStream(CERT_RESOURCE_PATH)?.readBytes()
        ?: File(CERT_FILE_PATH).takeIf { it.exists() }?.readBytes()
        ?: return null
    return CertificateFactory.getInstance("X.509")
        .generateCertificate(bytes.inputStream()) as X509Certificate
}
