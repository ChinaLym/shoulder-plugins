/**
 * 通用错误（2^14以下框架使用）错误码标识 = 0 的那部分
 * 多数通用错误，展示时仅为未知错误，dev环境可详情，堆栈，错误码映射表地址
 *
 * @author lym
 * 不能 implements ErrorCode
 */
public class MyErrorCodes {

    /**
     * @desc 未认证，需要认证后才能访问
     * @sug 先进行认证，再访问服务
     */
    public static final String AUTH_401_NEED_AUTH = "0x12323";
    /**
     * @desc 未认证，需要认证后才能访问
     * @sug 先进行认证，再访问服务
     */
    public static final String TIMEOUT = "0x13213";
}
