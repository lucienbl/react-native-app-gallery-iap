export = AppGalleryIap;
export as namespace AppGalleryIap;

namespace AppGalleryIap {
  /**
   * Initializes the AppGallery IAP. Rejects if the AppGallery IAP isn't supported.
   * 
   * @returns Promise, rejected if AppGallery IAP is not supported.
   */
  function initialize(): Promise<any>;

  /**
   * Fetches the registered products and returns the list.
   * 
   * @param priceType The price type of the product
   * 
   * @returns {Promise<Product[]>}
   */
  function fetchProducts(priceType: PriceType, productIds: string[]): Promise<Product[]>;

  /**
   * Initiates a product purchase.
   * 
   * @remark For a consumable product, parse purchaseToken and call {@link consumePurchase}.
   * @remark For non-consumables, Huawei IAP server returns confirmed purchase data by default.
   * 
   * @param priceType The price type of the product
   * @param productId The product ID to purchase
   * @param isTesting Set to true if you are testing
   * 
   * @returns {Promise<IapPurchase>}
   */
  function purchaseProduct(priceType: PriceType, productId: string, isTesting?: boolean = false): Promise<IapPurchase>;

  /**
   * Consumes a consumable purchase.
   * 
   * @param purchaseToken The purchase token from {@link IapPurchase}.
   * 
   * @returns {Promise<string>}
   */
  function consumePurchase(purchaseToken: string): Promise<string>;

  /**
   * Returns an array of owned in-app purchases
   * 
   * @param priceType The price type of the products to return.
   * 
   * @returns {Promise<IapPurchase[]>}
   */
  function fetchOwnedPurchases(priceType: PriceType): Promise<IapPurchase[]>;
}

/**
 * Represents a product
 */
export interface Product {
  id: string,
  currency: string,
  price: string,
  name: string,
  description: string
}

/**
 * Represents an in app purchase
 */
export interface IapPurchase {
  data: string,
  signature: string,
}

/**
 * Product fetch price types
 */
export enum PriceType {   
  CONSUMABLE,
  NON_CONSUMABLE,
  AUTO_RENEWABLE
}

/**
 * HMS Core IAP Error codes
 */
export enum IapErrorCode {
  ORDER_STATE_SUCCESS = 0,
  ORDER_STATE_FAILED = -1,
  ORDER_STATE_CANCEL = 60000,
  ORDER_STATE_PARAM_ERROR = 60001,
  ORDER_STATE_NET_ERROR = 60005,
  ORDER_VR_UNINSTALL_ERROR = 60020,
  ORDER_HWID_NOT_LOGIN = 60050,
  ORDER_PRODUCT_OWNED = 60051,
  ORDER_PRODUCT_NOT_OWNED = 60052,
  ORDER_PRODUCT_CONSUMED = 60053,
  ORDER_ACCOUNT_AREA_NOT_SUPPORTED = 60054,
  ORDER_NOT_ACCEPT_AGREEMENT = 60055,
  ORDER_HIGH_RISK_OPERATIONS = 60056,
}

/**
 * Module error codes
 */
export enum ErrorCode {
  E_NO_PRODUCTS_ADDED = "E_NO_PRODUCTS_ADDED",
  E_PURCHASE_INTENT_NO_DATA = "E_PURCHASE_INTENT_NO_DATA"
}