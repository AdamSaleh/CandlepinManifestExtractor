#Candlepin Manifest Extractor

##Usage
This is a small java program to extrack manifest from candlepin to be used with katello.
Manifest will contain entitlements for products Nature and Zoo.

To extract manifest use static function

org.candlepin.java.extractor.ManifestExtractor.extractManifest("admin", "admin", "katello.localdomain", "8443");

where parameters are username, password, candlepin address and port.
Program expects to have candlepin instance running on $HOST:$PORT/candlepin using https.
For this to work correctly you need to import cert to jave key store.

Usualy this can be done with,

sudo keytool -import -alias tcms -file SELFSIGNED_CERT.crt -keystore /usr/lib/jvm/java-1.6.0-openjdk-1.6.0.0.x86_64/jre/lib/security/cacerts

where default password is changeit
