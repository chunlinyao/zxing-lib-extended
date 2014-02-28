#zxing-android
This is a fork of Joe Simpson (@kennydude)'s work here: [https://github.com/kennydude/zxing-lib](https://github.com/kennydude/zxing-lib)

##Building the library for internal use
To build the library:

* Checkout Branch 'android-15'
* Build the library using the command 'gradle uploadArchives'. 

This will copy the library to your local ~/.m2 repository.

##Why?

* To fix a number of open issues which haven't been resolved in the upstream project. 
* Configures the Activity for portrait orientation to suit Wincor-Nixdorf's Albert Device.

##Using the library
Add the following dependency to your POM:  
````
<dependency>
    <groupId>com.skookum</groupId>
    <artifactId>zxing-android</artifactId>
    <version>1.0.0</version>
    <type>apklib</type>
</dependency>
````

##Building the library
Clone this repository and run `mvn clean install`.  Alternatively, run a m2e Maven Build in Eclipse with the same goals.  

##FAQ/Help
###In Eclipse, I see "plugin execution not covered by lifecycle" errors
Install the [m2e-android](http://rgladwell.github.io/m2e-android/) connector by clicking `Help` > `Eclipse Marketplace...` and searching for `Android m2e`.

###In Eclipse, I see "must override a superclass method" errors
Right-click the project, click `Properties` > `Java Compiler` and ensure that the `Compiler compliance level` is set to `1.6`.
