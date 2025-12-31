import { defineStore } from "pinia";

/**
 * 用户认证状态存储
 * 管理应用中的用户登录状态及相关操作
 */
export const userUserStore = defineStore('user', {
  // 状态定义
  state: () => ({
    user: null, // 当前登录用户信息，null表示未登录
  }),

  // 操作方法
  actions: {
    /**
     * 用户登录
     * @param {Object} user - 用户信息对象
     */
    login(user) {
      this.user = user;
    },

    /**
     * 用户登出
     */
    logout() {
      this.user = null;
    }
  }
});
