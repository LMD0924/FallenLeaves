package org.example.examback.util;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 个人信息验证工具类
 *
 * <p>提供手机号、邮箱、密码、身份证、姓名等常见个人信息的验证功能</p>
 *
 * @author 总会落叶
 * @date 2026/2/2
 * @version 1.0
 */
@Component
public class ValidationUtil {

    // ============ 预编译正则表达式（提高性能） ============

    // 中国大陆手机号（1开头，第二位3-9，共11位）
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^1[3-9]\\d{9}$");

    // 国际手机号（带国际区号，如+86 13800138000）
    private static final Pattern INTERNATIONAL_PHONE_PATTERN =
            Pattern.compile("^\\+[1-9]\\d{0,3}\\s?[1-9]\\d{4,14}$");

    // 邮箱地址
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");

    // 强密码（至少8位，包含大小写字母、数字、特殊字符）
    private static final Pattern STRONG_PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$");

    // 中等密码（至少6位，包含字母和数字）
    private static final Pattern MEDIUM_PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{6,}$");

    // 弱密码（至少6位）
    private static final Pattern WEAK_PASSWORD_PATTERN =
            Pattern.compile("^.{6,}$");

    // 中国大陆身份证（18位，最后一位可能是X）
    private static final Pattern ID_CARD_PATTERN =
            Pattern.compile("^[1-9]\\d{5}(19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[0-9Xx]$");

    // 中文姓名（2-4个中文字符）
    private static final Pattern CHINESE_NAME_PATTERN =
            Pattern.compile("^[\u4e00-\u9fa5]{2,4}$");

    // 英文姓名（允许中间空格，如 John Smith）
    private static final Pattern ENGLISH_NAME_PATTERN =
            Pattern.compile("^[A-Za-z]{2,20}(\\s[A-Za-z]{2,20})?$");

    // 用户名（字母开头，允许字母数字下划线，4-20位）
    private static final Pattern USERNAME_PATTERN =
            Pattern.compile("^[a-zA-Z][a-zA-Z0-9_]{3,19}$");

    // 验证码（6位数字）
    private static final Pattern VERIFICATION_CODE_PATTERN =
            Pattern.compile("^\\d{6}$");

    // URL地址
    private static final Pattern URL_PATTERN =
            Pattern.compile("^(https?://)?([\\w-]+\\.)+[\\w-]+(/[\\w-./?%&=]*)?$");

    // 日期（YYYY-MM-DD）
    private static final Pattern DATE_PATTERN =
            Pattern.compile("^\\d{4}-(0[1-9]|1[0-2])-(0[1-9]|[12]\\d|3[01])$");

    // IP地址
    private static final Pattern IP_PATTERN =
            Pattern.compile("^((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)$");

    // ============ 手机号验证 ============

    /**
     * 验证中国大陆手机号
     *
     * @param phone 手机号码
     * @return 验证结果
     */
    public ValidationResult validatePhone(String phone) {
        if (StringUtils.isBlank(phone)) {
            return ValidationResult.failure("手机号不能为空");
        }

        Matcher matcher = PHONE_PATTERN.matcher(phone);
        if (matcher.matches()) {
            return ValidationResult.success("手机号格式正确", phone);
        }

        return ValidationResult.failure("手机号格式不正确，应为11位数字，以13-19开头");
    }

    /**
     * 验证国际手机号
     *
     * @param phone 国际手机号（带国际区号）
     * @return 验证结果
     */
    public ValidationResult validateInternationalPhone(String phone) {
        if (StringUtils.isBlank(phone)) {
            return ValidationResult.failure("手机号不能为空");
        }

        Matcher matcher = INTERNATIONAL_PHONE_PATTERN.matcher(phone);
        if (matcher.matches()) {
            return ValidationResult.success("国际手机号格式正确", phone);
        }

        return ValidationResult.failure("国际手机号格式不正确，格式：+[国家代码] [手机号]");
    }

    /**
     * 验证手机号（自动识别国内/国际）
     *
     * @param phone 手机号码
     * @return 验证结果
     */
    public ValidationResult validatePhoneAuto(String phone) {
        if (StringUtils.isBlank(phone)) {
            return ValidationResult.failure("手机号不能为空");
        }

        if (phone.startsWith("+")) {
            return validateInternationalPhone(phone);
        } else {
            return validatePhone(phone);
        }
    }

    /**
     * 获取手机号运营商
     *
     * @param phone 手机号码
     * @return 运营商名称，未知返回 null
     */
    public String getPhoneOperator(String phone) {
        if (!validatePhone(phone).isSuccess()) {
            return null;
        }

        // 运营商号段
        String prefix = phone.substring(0, 3);
        String second = phone.substring(0, 4);

        // 中国移动
        if (prefix.startsWith("134") || prefix.startsWith("135") ||
                prefix.startsWith("136") || prefix.startsWith("137") ||
                prefix.startsWith("138") || prefix.startsWith("139") ||
                second.equals("1470") || prefix.startsWith("150") ||
                prefix.startsWith("151") || prefix.startsWith("152") ||
                prefix.startsWith("157") || prefix.startsWith("158") ||
                prefix.startsWith("159") || prefix.startsWith("178") ||
                prefix.startsWith("182") || prefix.startsWith("183") ||
                prefix.startsWith("184") || prefix.startsWith("187") ||
                prefix.startsWith("188") || prefix.startsWith("198")) {
            return "中国移动";
        }

        // 中国联通
        if (prefix.startsWith("130") || prefix.startsWith("131") ||
                prefix.startsWith("132") || prefix.startsWith("145") ||
                prefix.startsWith("155") || prefix.startsWith("156") ||
                prefix.startsWith("166") || prefix.startsWith("175") ||
                prefix.startsWith("176") || prefix.startsWith("185") ||
                prefix.startsWith("186")) {
            return "中国联通";
        }

        // 中国电信
        if (prefix.startsWith("133") || prefix.startsWith("149") ||
                prefix.startsWith("153") || prefix.startsWith("173") ||
                prefix.startsWith("177") || prefix.startsWith("180") ||
                prefix.startsWith("181") || prefix.startsWith("189") ||
                prefix.startsWith("199")) {
            return "中国电信";
        }

        // 虚拟运营商
        if (prefix.startsWith("170") || prefix.startsWith("171") ||
                prefix.startsWith("172")) {
            return "虚拟运营商";
        }

        return "未知运营商";
    }

    // ============ 邮箱验证 ============

    /**
     * 验证邮箱地址
     *
     * @param email 邮箱地址
     * @return 验证结果
     */
    public ValidationResult validateEmail(String email) {
        if (StringUtils.isBlank(email)) {
            return ValidationResult.failure("邮箱不能为空");
        }

        if (email.length() > 100) {
            return ValidationResult.failure("邮箱地址过长");
        }

        Matcher matcher = EMAIL_PATTERN.matcher(email);
        if (matcher.matches()) {
            return ValidationResult.success("邮箱格式正确", email);
        }

        return ValidationResult.failure("邮箱格式不正确");
    }

    /**
     * 获取邮箱域名
     *
     * @param email 邮箱地址
     * @return 邮箱域名，验证失败返回 null
     */
    public String getEmailDomain(String email) {
        ValidationResult result = validateEmail(email);
        if (!result.isSuccess()) {
            return null;
        }

        return email.substring(email.indexOf("@") + 1);
    }

    // ============ 密码验证 ============

    /**
     * 密码强度枚举
     */
    public enum PasswordStrength {
        WEAK,      // 弱
        MEDIUM,    // 中
        STRONG,    // 强
        INVALID    // 无效
    }

    /**
     * 验证密码强度
     *
     * @param password 密码
     * @return 密码强度
     */
    public PasswordStrength checkPasswordStrength(String password) {
        if (StringUtils.isBlank(password)) {
            return PasswordStrength.INVALID;
        }

        if (STRONG_PASSWORD_PATTERN.matcher(password).matches()) {
            return PasswordStrength.STRONG;
        } else if (MEDIUM_PASSWORD_PATTERN.matcher(password).matches()) {
            return PasswordStrength.MEDIUM;
        } else if (WEAK_PASSWORD_PATTERN.matcher(password).matches()) {
            return PasswordStrength.WEAK;
        } else {
            return PasswordStrength.INVALID;
        }
    }

    /**
     * 验证密码（根据强度要求）
     *
     * @param password 密码
     * @param minStrength 要求的最小强度
     * @return 验证结果
     */
    public ValidationResult validatePassword(String password, PasswordStrength minStrength) {
        if (StringUtils.isBlank(password)) {
            return ValidationResult.failure("密码不能为空");
        }

        PasswordStrength strength = checkPasswordStrength(password);

        switch (minStrength) {
            case STRONG:
                if (strength == PasswordStrength.STRONG) {
                    return ValidationResult.success("密码强度：强", password);
                }
                return ValidationResult.failure("密码强度不足，需包含大小写字母、数字、特殊字符，至少8位");

            case MEDIUM:
                if (strength == PasswordStrength.STRONG || strength == PasswordStrength.MEDIUM) {
                    return ValidationResult.success("密码强度：" + strength, password);
                }
                return ValidationResult.failure("密码强度不足，需包含字母和数字，至少6位");

            case WEAK:
                if (strength != PasswordStrength.INVALID) {
                    return ValidationResult.success("密码强度：" + strength, password);
                }
                return ValidationResult.failure("密码无效，至少需要6位");

            default:
                return ValidationResult.failure("密码强度要求无效");
        }
    }

    /**
     * 验证两次输入的密码是否一致
     *
     * @param password 密码
     * @param confirmPassword 确认密码
     * @return 验证结果
     */
    public ValidationResult validatePasswordConfirm(String password, String confirmPassword) {
        if (StringUtils.isBlank(password)) {
            return ValidationResult.failure("密码不能为空");
        }

        if (StringUtils.isBlank(confirmPassword)) {
            return ValidationResult.failure("确认密码不能为空");
        }

        if (password.equals(confirmPassword)) {
            return ValidationResult.success("密码一致", password);
        }

        return ValidationResult.failure("两次输入的密码不一致");
    }

    // ============ 身份证验证 ============

    /**
     * 验证中国大陆身份证号
     *
     * @param idCard 身份证号码
     * @return 验证结果
     */
    public ValidationResult validateIdCard(String idCard) {
        if (StringUtils.isBlank(idCard)) {
            return ValidationResult.failure("身份证号不能为空");
        }

        // 基本格式验证
        Matcher matcher = ID_CARD_PATTERN.matcher(idCard);
        if (!matcher.matches()) {
            return ValidationResult.failure("身份证号格式不正确");
        }

        // 校验码验证（第18位）
        if (!validateIdCardCheckCode(idCard)) {
            return ValidationResult.failure("身份证号校验码错误");
        }

        return ValidationResult.success("身份证号格式正确", idCard);
    }

    /**
     * 验证身份证校验码
     *
     * @param idCard 身份证号码
     * @return 校验结果
     */
    private boolean validateIdCardCheckCode(String idCard) {
        if (idCard.length() != 18) {
            return false;
        }

        // 加权因子
        int[] weight = {7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2};
        // 校验码对应值
        char[] checkCode = {'1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2'};

        int sum = 0;
        for (int i = 0; i < 17; i++) {
            sum += (idCard.charAt(i) - '0') * weight[i];
        }

        int mod = sum % 11;
        char expectedCheckCode = checkCode[mod];

        return Character.toUpperCase(idCard.charAt(17)) == expectedCheckCode;
    }

    /**
     * 从身份证号提取生日
     *
     * @param idCard 身份证号码
     * @return 生日字符串（YYYY-MM-DD），验证失败返回 null
     */
    public String extractBirthdayFromIdCard(String idCard) {
        ValidationResult result = validateIdCard(idCard);
        if (!result.isSuccess()) {
            return null;
        }

        String year = idCard.substring(6, 10);
        String month = idCard.substring(10, 12);
        String day = idCard.substring(12, 14);

        return year + "-" + month + "-" + day;
    }

    /**
     * 从身份证号提取性别
     *
     * @param idCard 身份证号码
     * @return 性别（1=男，2=女），验证失败返回 0
     */
    public int extractGenderFromIdCard(String idCard) {
        ValidationResult result = validateIdCard(idCard);
        if (!result.isSuccess()) {
            return 0;
        }

        // 第17位为性别位，奇数为男，偶数为女
        char genderChar = idCard.charAt(16);
        int genderNum = genderChar - '0';

        return (genderNum % 2 == 1) ? 1 : 2;
    }

    // ============ 姓名验证 ============

    /**
     * 验证姓名（自动识别中英文）
     *
     * @param name 姓名
     * @return 验证结果
     */
    public ValidationResult validateName(String name) {
        if (StringUtils.isBlank(name)) {
            return ValidationResult.failure("姓名不能为空");
        }

        if (name.length() < 2 || name.length() > 40) {
            return ValidationResult.failure("姓名长度应在2-40个字符之间");
        }

        // 判断是否为中文
        if (isChinese(name)) {
            return validateChineseName(name);
        } else {
            return validateEnglishName(name);
        }
    }

    /**
     * 验证中文姓名
     *
     * @param name 中文姓名
     * @return 验证结果
     */
    public ValidationResult validateChineseName(String name) {
        if (StringUtils.isBlank(name)) {
            return ValidationResult.failure("姓名不能为空");
        }

        Matcher matcher = CHINESE_NAME_PATTERN.matcher(name);
        if (matcher.matches()) {
            return ValidationResult.success("中文姓名格式正确", name);
        }

        return ValidationResult.failure("中文姓名应为2-4个汉字");
    }

    /**
     * 验证英文姓名
     *
     * @param name 英文姓名
     * @return 验证结果
     */
    public ValidationResult validateEnglishName(String name) {
        if (StringUtils.isBlank(name)) {
            return ValidationResult.failure("姓名不能为空");
        }

        Matcher matcher = ENGLISH_NAME_PATTERN.matcher(name);
        if (matcher.matches()) {
            return ValidationResult.success("英文姓名格式正确", name);
        }

        return ValidationResult.failure("英文姓名格式不正确，应包含字母，允许中间空格");
    }

    /**
     * 判断字符串是否主要为中文字符
     *
     * @param str 字符串
     * @return 是否中文
     */
    private boolean isChinese(String str) {
        int chineseCount = 0;
        for (char c : str.toCharArray()) {
            if (Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN) {
                chineseCount++;
            }
        }
        return chineseCount * 2 >= str.length(); // 中文字符占一半以上
    }

    // ============ 用户名验证 ============

    /**
     * 验证用户名
     *
     * @param username 用户名
     * @return 验证结果
     */
    public ValidationResult validateUsername(String username) {
        if (StringUtils.isBlank(username)) {
            return ValidationResult.failure("用户名不能为空");
        }

        if (username.length() < 4 || username.length() > 20) {
            return ValidationResult.failure("用户名长度应在4-20个字符之间");
        }

        Matcher matcher = USERNAME_PATTERN.matcher(username);
        if (matcher.matches()) {
            return ValidationResult.success("用户名格式正确", username);
        }

        return ValidationResult.failure("用户名必须以字母开头，可包含字母、数字、下划线");
    }

    // ============ 验证码验证 ============

    /**
     * 验证验证码格式
     *
     * @param code 验证码
     * @param length 验证码长度（默认6位）
     * @return 验证结果
     */
    public ValidationResult validateVerificationCode(String code, int length) {
        if (StringUtils.isBlank(code)) {
            return ValidationResult.failure("验证码不能为空");
        }

        if (length == 6) {
            Matcher matcher = VERIFICATION_CODE_PATTERN.matcher(code);
            if (matcher.matches()) {
                return ValidationResult.success("验证码格式正确", code);
            }
        } else {
            Pattern pattern = Pattern.compile("^\\d{" + length + "}$");
            if (pattern.matcher(code).matches()) {
                return ValidationResult.success("验证码格式正确", code);
            }
        }

        return ValidationResult.failure("验证码应为" + length + "位数字");
    }

    /**
     * 验证验证码格式（默认6位）
     */
    public ValidationResult validateVerificationCode(String code) {
        return validateVerificationCode(code, 6);
    }

    // ============ 其他通用验证 ============

    /**
     * 验证URL地址
     *
     * @param url URL地址
     * @return 验证结果
     */
    public ValidationResult validateUrl(String url) {
        if (StringUtils.isBlank(url)) {
            return ValidationResult.failure("URL不能为空");
        }

        Matcher matcher = URL_PATTERN.matcher(url);
        if (matcher.matches()) {
            return ValidationResult.success("URL格式正确", url);
        }

        return ValidationResult.failure("URL格式不正确");
    }

    /**
     * 验证日期格式（YYYY-MM-DD）
     *
     * @param date 日期字符串
     * @return 验证结果
     */
    public ValidationResult validateDate(String date) {
        if (StringUtils.isBlank(date)) {
            return ValidationResult.failure("日期不能为空");
        }

        Matcher matcher = DATE_PATTERN.matcher(date);
        if (matcher.matches()) {
            return ValidationResult.success("日期格式正确", date);
        }

        return ValidationResult.failure("日期格式不正确，应为YYYY-MM-DD");
    }

    /**
     * 验证IP地址
     *
     * @param ip IP地址
     * @return 验证结果
     */
    public ValidationResult validateIp(String ip) {
        if (StringUtils.isBlank(ip)) {
            return ValidationResult.failure("IP地址不能为空");
        }

        Matcher matcher = IP_PATTERN.matcher(ip);
        if (matcher.matches()) {
            return ValidationResult.success("IP地址格式正确", ip);
        }

        return ValidationResult.failure("IP地址格式不正确");
    }

    /**
     * 验证整数
     *
     * @param number 数字字符串
     * @param min 最小值（包含）
     * @param max 最大值（包含）
     * @return 验证结果
     */
    public ValidationResult validateInteger(String number, Integer min, Integer max) {
        if (StringUtils.isBlank(number)) {
            return ValidationResult.failure("数字不能为空");
        }

        try {
            int value = Integer.parseInt(number);

            if (min != null && value < min) {
                return ValidationResult.failure("数字不能小于" + min);
            }

            if (max != null && value > max) {
                return ValidationResult.failure("数字不能大于" + max);
            }

            return ValidationResult.success("数字验证通过", value);
        } catch (NumberFormatException e) {
            return ValidationResult.failure("请输入有效的整数");
        }
    }

    /**
     * 验证字符串长度
     *
     * @param str 字符串
     * @param minLength 最小长度
     * @param maxLength 最大长度
     * @return 验证结果
     */
    public ValidationResult validateStringLength(String str, int minLength, int maxLength) {
        if (StringUtils.isBlank(str)) {
            if (minLength > 0) {
                return ValidationResult.failure("字符串不能为空");
            }
            return ValidationResult.success("字符串为空但允许", "");
        }

        int length = str.length();

        if (length < minLength) {
            return ValidationResult.failure("长度不能小于" + minLength + "个字符");
        }

        if (length > maxLength) {
            return ValidationResult.failure("长度不能大于" + maxLength + "个字符");
        }

        return ValidationResult.success("字符串长度验证通过", str);
    }

    /**
     * 验证是否为数字
     *
     * @param str 字符串
     * @return 验证结果
     */
    public ValidationResult validateNumeric(String str) {
        if (StringUtils.isBlank(str)) {
            return ValidationResult.failure("字符串不能为空");
        }

        try {
            Double.parseDouble(str);
            return ValidationResult.success("是有效数字", str);
        } catch (NumberFormatException e) {
            return ValidationResult.failure("请输入有效的数字");
        }
    }

    // ============ 批量验证 ============

    /**
     * 批量验证用户注册信息
     *
     * @param phone 手机号
     * @param email 邮箱
     * @param password 密码
     * @param confirmPassword 确认密码
     * @param username 用户名
     * @param name 姓名
     * @return 批量验证结果
     */
    public BatchValidationResult validateUserRegistration(
            String phone, String email, String password,
            String confirmPassword, String username, String name) {

        BatchValidationResult batchResult = new BatchValidationResult();

        // 验证手机号
        ValidationResult phoneResult = validatePhone(phone);
        batchResult.addResult("phone", phoneResult);

        // 验证邮箱
        if (StringUtils.isNotBlank(email)) {
            ValidationResult emailResult = validateEmail(email);
            batchResult.addResult("email", emailResult);
        }

        // 验证密码
        ValidationResult passwordResult = validatePassword(password, PasswordStrength.MEDIUM);
        batchResult.addResult("password", passwordResult);

        // 验证确认密码
        if (passwordResult.isSuccess()) {
            ValidationResult confirmResult = validatePasswordConfirm(password, confirmPassword);
            batchResult.addResult("confirmPassword", confirmResult);
        }

        // 验证用户名
        if (StringUtils.isNotBlank(username)) {
            ValidationResult usernameResult = validateUsername(username);
            batchResult.addResult("username", usernameResult);
        }

        // 验证姓名
        if (StringUtils.isNotBlank(name)) {
            ValidationResult nameResult = validateName(name);
            batchResult.addResult("name", nameResult);
        }

        return batchResult;
    }

    // ============ 内部类 ============

    /**
     * 验证结果封装类
     */
    public static class ValidationResult {
        private boolean success;
        private String message;
        private Object data;

        private ValidationResult(boolean success, String message, Object data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public static ValidationResult success(String message) {
            return new ValidationResult(true, message, null);
        }

        public static ValidationResult success(String message, Object data) {
            return new ValidationResult(true, message, data);
        }

        public static ValidationResult failure(String message) {
            return new ValidationResult(false, message, null);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public Object getData() {
            return data;
        }

        @SuppressWarnings("unchecked")
        public <T> T getData(Class<T> clazz) {
            return (T) data;
        }
    }

    /**
     * 批量验证结果封装类
     */
    public static class BatchValidationResult {
        private final Map<String, ValidationResult> results = new HashMap<>();
        private boolean overallSuccess = true;

        public void addResult(String field, ValidationResult result) {
            results.put(field, result);
            if (!result.isSuccess()) {
                overallSuccess = false;
            }
        }

        public boolean isOverallSuccess() {
            return overallSuccess;
        }

        public Map<String, ValidationResult> getResults() {
            return results;
        }

        public ValidationResult getResult(String field) {
            return results.get(field);
        }

        public List<String> getErrorMessages() {
            return results.entrySet().stream()
                    .filter(entry -> !entry.getValue().isSuccess())
                    .map(entry -> entry.getKey() + ": " + entry.getValue().getMessage())
                    .collect(Collectors.toList());
        }

        public String getCombinedErrorMessage() {
            return String.join("; ", getErrorMessages());
        }
    }
}