package com.itechart.project.https

import akka.http.scaladsl.{ConnectionContext, HttpsConnectionContext}
import io.circe.generic.JsonCodec

import java.io.{File, FileInputStream, InputStream}
import java.security.{KeyStore, SecureRandom}
import javax.net.ssl.{KeyManagerFactory, SSLContext, TrustManagerFactory}

object HttpsContext {

  @JsonCodec
  case class HttpsConfiguration(secret: String)

  def httpsContext(httpsConfiguration: HttpsConfiguration): HttpsConnectionContext = {
    val ks:           KeyStore    = KeyStore.getInstance("PKCS12")
    val keyStoreFile: InputStream = new FileInputStream(new File("src/main/resources/keystore.pkcs12"))
    val password:     Array[Char] = httpsConfiguration.secret.toCharArray
    ks.load(keyStoreFile, password)

    val keyManagerFactory: KeyManagerFactory = KeyManagerFactory.getInstance("SunX509")
    keyManagerFactory.init(ks, password)

    val trustManagerFactory: TrustManagerFactory = TrustManagerFactory.getInstance("SunX509")
    trustManagerFactory.init(ks)

    val sslContext: SSLContext = SSLContext.getInstance("TLS")
    sslContext.init(keyManagerFactory.getKeyManagers, trustManagerFactory.getTrustManagers, new SecureRandom)

    ConnectionContext.httpsServer(sslContext)
  }

}
