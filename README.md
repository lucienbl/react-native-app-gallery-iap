# react-native-app-gallery-iap

## Installation
`$ yarn add react-native-app-gallery-iap`

### Additional steps
Make sure to read the AppGallery Documentation. The following steps are the most important ones :
1. [Configure the app](https://developer.huawei.com/consumer/en/doc/development/HMS-Guides/iap-configuring-appGallery-connect)
1. [Add the configuration file](https://developer.huawei.com/consumer/en/service/josp/agc/index.html) in `android/app/agconnect-services.json`
3. Add the huawei maven repo to your project level `build.gradle` file :
  ```groovy
  allprojects {
      repositories {
          maven {
            url 'https://developer.huawei.com/repo/'
          }
      }
  }
  ```
4. Add the huawei maven repo to your project level `build.gradle` file (buildscript) :
  ```groovy
  buildscript {
      repositories {
          maven {
            url 'https://developer.huawei.com/repo/'
          }
      }
  }
  ```
5. Add the huawei classpath to the project level `build.gradle` file :
  ```groovy
  buildscript {
      dependencies {
          classpath('com.huawei.agconnect:agcp:1.3.1.300')
      }
  }
  ```
6. Add the following into your app level `build.gradle` implementations :
  ```groovy
  implementation 'com.huawei.hms:iap:4.0.4.300'
  ```
7. Apply the plugin at the bottom of your app level `build.gradle` file :
  ```groovy
  apply plugin: 'com.huawei.agconnect'
  ```


## Example
```js
import AppGalleryIap, { PriceType } from "react-native-app-gallery-iap";

try {
  await AppGalleryIap.initialize(); // Initializes, if throws error AppGallery IAP is probably not supported
  await AppGalleryIap.fetchProducts(PriceType.CONSUMABLE, ["product_id_1"])); // Returns Promise<Product[]>
  await AppGalleryIap.fetchOwnedPurchases(PriceType.CONSUMABLE); // Returns Promise<any>
} catch (e) {
  throw e;
}
```

See `index.d.ts` for detailed method documentation with types.