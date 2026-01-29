package org.example.examback.util;

import org.example.examback.entity.User;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

import java.beans.PropertyDescriptor;
import java.util.HashSet;
import java.util.Set;

public class UserMergeUtil {

    /**
     * 合并两个用户对象，保留currentUser的非空值，用updateUser的非空值覆盖
     */
    public static User merge(User updateUser, User currentUser) {
        if (updateUser == null || currentUser == null) {
            return currentUser;
        }

        // 创建一个新的对象来存储合并结果
        User result = new User();

        // 先将当前用户的所有值复制到结果
        copyNonNullProperties(currentUser, result);

        // 再用更新用户的非空值覆盖
        copyNonNullProperties(updateUser, result);

        // 确保ID不被覆盖
        result.setId(currentUser.getId());

        return result;
    }

    /**
     * 复制源对象的非空属性到目标对象
     */
    private static void copyNonNullProperties(Object source, Object target) {
        BeanWrapper src = new BeanWrapperImpl(source);
        BeanWrapper trg = new BeanWrapperImpl(target);

        PropertyDescriptor[] pds = src.getPropertyDescriptors();

        for (PropertyDescriptor pd : pds) {
            String propertyName = pd.getName();

            // 跳过class属性和null值
            if ("class".equals(propertyName)) {
                continue;
            }

            Object srcValue = src.getPropertyValue(propertyName);
            if (srcValue != null && trg.isWritableProperty(propertyName)) {
                trg.setPropertyValue(propertyName, srcValue);
            }
        }
    }

    /**
     * 获取对象的空属性名数组
     */
    public static String[] getNullPropertyNames(Object source) {
        BeanWrapper src = new BeanWrapperImpl(source);
        PropertyDescriptor[] pds = src.getPropertyDescriptors();

        Set<String> emptyNames = new HashSet<>();
        for (PropertyDescriptor pd : pds) {
            Object srcValue = src.getPropertyValue(pd.getName());
            if (srcValue == null) {
                emptyNames.add(pd.getName());
            }
        }

        return emptyNames.toArray(new String[0]);
    }
}