import org.shoulder.core.exception.BaseRuntimeException;
import org.shoulder.core.exception.ErrorCode;

/**
 * 不建议这么用
 *
 * @author lym
 * @desc 不建议
 * @sug 不建议
 */
public class BusinessRuntimeException extends BaseRuntimeException implements ErrorCode {
    
    private static final long serialVersionUID = -3940675117866395789L;

    public BusinessRuntimeException() {
        super("0x12312312", "message");
    }

    public BusinessRuntimeException(Throwable cause, Object... args) {
        super("0x12312312", "message", cause, args);
    }

    public BusinessRuntimeException(Object... args) {
        super("0x12312312", "message", args);
    }

}
