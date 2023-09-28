
#if __has_include("RCTBridgeModule.h")
#import "RCTBridgeModule.h"
#else
#import <React/RCTBridgeModule.h>
#endif

@import "PGW"

@interface RNMy2c2pSdk : NSObject <RCTBridgeModule>

@property (nonatomic,strong) PGW *my2c2pSDK;

@end
  
