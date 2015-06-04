# alitvgamesdk-serverdemo

BaodianHelper
-------------

BaodianHelper.{java|js|php} are sign helpers. They are libraries that can be imported, as well as standalone applications:

    Usage:
        node BaodianHelper secret key1 value1 key2 value2 ...
        java BaodianHelper secret key1 value1 key2 value2 ...
        
where,
- secret is BAODIAN_SECRET allocated by http://open.aliplay.com
- key and value are defined in [HTTP API](http://kaiwangchen.gitbooks.io/alitvgamesdk-tutorial/content/api/http.html) from `alitvgamesdk-tutorial`

serverdemo webapp in Java
-------------------------

Find in `java` directory a maven project.

    cd java
    mvn package
    
A successful build creates `serverdemo.war` which can be put in Tomcat `webapps` directory and called like,

    http://localhost:8080/serverdemo/notify?sign=4c8434ce16e71eaf418796f6cd951085&app_order_id=123456&coin_order_id=12345&consume_amount=1000&credit_amount=50&is_success=T&ts=1431675944825
    
Since the HTTP call is issued by Baodian server in production environment, you have to make your server accessible over Internet.

