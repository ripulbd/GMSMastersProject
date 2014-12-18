GSMastersProject
=================

GMS Masters Project

1. How to run topic modelling

./bin/Compile.sh
./bin/start-gms.sh

lib: https://www.dropbox.com/sh/1u9pmo2mhkfnvqs/AAAaw62NKgmUWGezqHB1LNWBa?dl=0

=== How to install Topic Modeling ===
1) Download Mallet (http://mallet.cs.umass.edu/index.php)
2) Use Mallet to train the data(Make sure the data you have put into the data folder)
    ./bin/mallet import-dir --input ./data/ --output newData.mallet --keep-sequence --remove-stopwords
    ./bin/mallet train-topics  --input newData.mallet --num-topics 100 --output-state topic-state.gz --output-topic-keys newdata_keys.txt --output-doc-topics newdata_compostion.txt --optimize-interval 20 --num-top-words 10

3) Download Eclipse IDE for Java EE Developers (http://www.eclipse.org/downloads/)
4) Import "Modeling" packages to eclipse
5) Change the key of AlchemyAPI and uClassifyAPI:
        AlchemyAPI: ExAlchemy -- private static final String APIKEY = "3548f080e9084fd16bb177573876f17948bfdb36";
        uClassifyAPI: uClassify -- private static String APIKEY = "lFrLBvoFpQSuEsRBpMDSjQ0fQc";
6) Download and import these jar files to classpath
javax.json-1.0.4.jar
javax-json-api-1.0.jar
uclassify-java.sdk.jar
unirest-java.1.3.0.jar
httpclient-4.3.6.jar
stanford-parser.jar
7) Run the code
    1. ExMain -- Get the ontology
    2. ExMallet -- Process the result of Mallet
	
=== How to install Summarization Server ===
1) Download Eclipse IDE for Java EE Developers (http://www.eclipse.org/downloads/?)
2) Import "Summarization" package to eclipse
3) Download Tomcat and setup to your project (http://tomcat.apache.org/) , (http://www.coreservlets.com/Apache-Tomcat-Tutorial/tomcat-7-with-eclipse.html)
4) Create Tomcat run configuration and apply these jar file to classpath 
- antlr-2.7.2.jar
- lucence-analyzer-3.5.0.jar
- lucence-analyzer-3.0.2.jar
- lucence-core-3.5.0.jar
- lucence-core-3.6.2.jar
- lucence-core-4.10.0.jar
- mongo-2.10.0.jar
- json-lib-0.9.jar
- ezmorph-1.0.6.jar
- commons-logging-1.0.4.jar
- commons-beanutils-1.7.0.jar
- commons-cli-1.0.jar
- commons-codec-1.2.jar
- commons-collections-3.1.jar
- commons-httpclient-3.0.jar
- commons-io-1.2.jar
- commons-lang-2.5.jar
- commons-math-2.0.jar
- classifier4j-0.6.jar
- log4j-1.2.14.jar
5) Run Tomcat server (server.java)
*** you can change database and collection name at ScoreOfSentence.java at line 399 and 401 ***

=== How to install Visualization Part (Website) ===
1) Install GoLang (https://golang.org/doc/install)
2) Setup GoPath (https://github.com/golang/go/wiki/GOPATH)
3) Install plug-in (can see more detail in the timeline.go)
	Plugins:
		1) mgo for connect with mongodb (https://labix.org/mgo)
		> go get gopkg.in/mgo.v2
		   
		2) Gorilla for session (http://www.gorillatoolkit.org/pkg/sessions) 
		> go get github.com/gorilla/sessions
		
		3) Snowball steming algorithm (https://github.com/kljensen/snowball)
		> go get github.com/kljensen/snowball
4) Setup MongoDB
5) Import "GMS-Golang-V" package to your IDE, if you use eclipse, you can use GoClipse (https://code.google.com/p/goclipse/)
6) Following instruction in guide in timeline.go (change some constant name)
7) Start MongoDB server (mongod --dbpath "%DBPATH%")
8) Start Summarization Server
9) Run timeline.go
10) Access "localhost:8090" on your browser.

** put all image in Visualization/GMS-Golang-V/resources/images

=== How to install and setup MongoDB ===
1) Install MongoDB from (http://www.mongodb.org/)
2) Create your "%DBPATH%" on your space
3) Start MongoDB server by type following command on CMD
 > mongod --dbpath "%DBPATH%"
4) Installation database in another CMD (these are suggestion commands)
 > mongoimport --db demo --collection records --file myRecords2Months.json
 > mongoimport --db demo --collection modeling --file modeling.json
 > mongoimport --db demo --collection keywords --file keyword.json
** you can find more details in timeline.go **