# Sample Spring Boot service

This is a sample Helloword application. 

# How to Run 

After building  deploy the produced .war artifact to a local Tomcat server. 
Run `gradlew helloworld-spring:tomcatRun` with these additional parameters:
 
`-Djavax.net.ssl.trustStore="{your-project-directory}\api-layer\keystore\localhost\localhost.truststore.p12" 
-Djavax.net.ssl.trustStorePassword="password"`.

For more information on how to configure Tomcat server check the file docs > local-configuration.md and follow the same steps as Helloworld jersey 

# How to use

You can see this application registered to your local running catalog on Tile "Sample API Mediation Layer Applications"

For API requests use endpoints "/greeting" for a generic greet or "greeting/{name}" for a greet returning your input {name}