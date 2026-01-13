<template>
  <div class="min-h-screen" :class="isDark ? 'bg-black' : 'bg-gradient-to-br from-blue-50 to-indigo-100'">
    <!-- 导航栏 -->
    <nav class="backdrop-blur-md border-b sticky top-0 z-50" :class="isDark ? 'bg-black/80 border-white/10' : 'bg-white/80 border-gray-200/50'">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="flex justify-between h-16">
          <div class="flex items-center">
            <div class="text-2xl font-bold flex items-center" :class="isDark ? 'text-white' : 'text-gray-900'">
              <svg xmlns="http://www.w3.org/2000/svg" class="w-8 h-8 mr-3" :class="isDark ? 'text-indigo-400' : 'text-indigo-600'" viewBox="0 0 24 24" fill="currentColor">
                <path d="M12 3v10.55c-.59-.34-1.27-.55-2-.55c-2.21 0-4 1.79-4 4s1.79 4 4 4s4-1.79 4-4V7h4V3m-7 19c-1.66 0-3-1.34-3-3s1.34-3 3-3s3 1.34 3 3s-1.34 3-3 3z"/>
              </svg>
              消息墙
            </div>
          </div>
          <div class="flex items-center space-x-4">
            <div class="flex items-center">
              <img v-if="currentUser?.avatar" :src="currentUser.avatar" class="w-8 h-8 rounded-full mr-2 border-2" :class="isDark ? 'border-indigo-500/50' : 'border-indigo-400/50'" alt="头像">
              <span class="font-medium" :class="isDark ? 'text-white' : 'text-gray-900'">{{ currentUser?.account || '用户' }}</span>
            </div>
            <button
              @click="showSendModal = true"
              class="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 rounded-lg text-white transition-colors flex items-center"
            >
              <svg class="w-5 h-5 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8"/>
              </svg>
              发送消息
            </button>
            <button
              @click="handleLogout"
              class="px-4 py-2 rounded-lg transition-colors"
              :class="isDark ? 'bg-white/5 hover:bg-white/10 border border-white/10 text-white' : 'bg-gray-100 hover:bg-gray-200 text-gray-700'"
            >
              退出
            </button>
          </div>
        </div>
      </div>
    </nav>

    <!-- 标签页切换 -->
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 mt-6">
      <div class="flex border-b" :class="isDark ? 'border-white/10' : 'border-gray-200'">
        <button
          @click="activeTab = 'received'"
          :class="[
            'px-6 py-3 text-sm font-medium rounded-t-lg transition',
            activeTab === 'received'
              ? isDark
                ? 'bg-white/5 border border-b-0 border-white/10 text-indigo-400'
                : 'bg-white border border-b-0 border-gray-200 text-indigo-600'
              : isDark
                ? 'text-white/50 hover:text-white'
                : 'text-gray-500 hover:text-gray-700'
          ]"
        >
          <span class="flex items-center">
            <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 8l7.89 4.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z"/>
            </svg>
            收到的消息
          </span>
        </button>
        <button
          @click="activeTab = 'sent'"
          :class="[
            'px-6 py-3 text-sm font-medium rounded-t-lg transition ml-2',
            activeTab === 'sent'
              ? isDark
                ? 'bg-white/5 border border-b-0 border-white/10 text-indigo-400'
                : 'bg-white border border-b-0 border-gray-200 text-indigo-600'
              : isDark
                ? 'text-white/50 hover:text-white'
                : 'text-gray-500 hover:text-gray-700'
          ]"
        >
          <span class="flex items-center">
            <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8"/>
            </svg>
            我发送的
          </span>
        </button>
      </div>
    </div>

    <!-- 消息墙主体 -->
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 mt-6">
      <!-- 收到的消息 -->
      <div v-if="activeTab === 'received'" class="backdrop-blur-md rounded-2xl p-6 border" :class="isDark ? 'bg-gradient-to-br from-white/5 to-white/3 border-white/10' : 'bg-gray-50 border-gray-200'">
        <div class="flex justify-between items-center mb-6">
          <h2 class="text-2xl font-bold" :class="isDark ? 'text-white' : 'text-gray-900'">收到的消息</h2>
          <button
            @click="loadReceivedBlessings"
            class="text-sm flex items-center transition-colors"
            :class="isDark ? 'text-indigo-400 hover:text-indigo-300' : 'text-indigo-600 hover:text-indigo-800'"
          >
            <svg class="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"/>
            </svg>
            刷新
          </button>
        </div>

        <!-- 收到的消息列表 -->
        <template v-if="receivedBlessings.length > 0">
          <div class="space-y-4">
            <div
              v-for="blessing in receivedBlessings"
              :key="blessing.id"
              class="border rounded-lg p-4 transition-all hover:scale-[1.01]"
              :class="isDark ? 'bg-white/5 border-white/10 hover:border-indigo-400/30' : 'bg-white border-gray-200 hover:border-indigo-300'"
            >
              <div class="flex justify-between items-start">
                <div class="flex items-start space-x-3 flex-1">
                  <!-- 头像 -->
                  <img
                    v-if="blessing.userAvatar"
                    :src="blessing.userAvatar"
                    class="w-10 h-10 rounded-full border-2"
                    :class="isDark ? 'border-indigo-500/50' : 'border-indigo-400/50'"
                    alt="头像"
                  >
                  <div v-else class="w-10 h-10 rounded-full flex items-center justify-center text-white font-bold"
                       :class="isDark ? 'bg-indigo-500/30' : 'bg-indigo-500'">
                    {{ blessing.userName?.charAt(0) || '?' }}
                  </div>

                  <!-- 内容 -->
                  <div class="flex-1">
                    <div class="flex items-center">
                      <span class="font-bold" :class="isDark ? 'text-white' : 'text-gray-900'">{{ blessing.userName || '匿名用户' }}</span>
                      <span class="mx-2" :class="isDark ? 'text-white/40' : 'text-gray-400'">•</span>
                      <span class="text-sm" :class="isDark ? 'text-white/60' : 'text-gray-500'">{{ formatTime(blessing.time) }}</span>
                    </div>
                    <p class="mt-2" :class="isDark ? 'text-white/80' : 'text-gray-700'">{{ blessing.content }}</p>
                    <div class="mt-2">
                      <span class="inline-flex items-center px-2 py-1 rounded-full text-xs"
                            :class="blessing.recipientId === 0 
                              ? isDark ? 'bg-blue-500/20 text-blue-400' : 'bg-blue-100 text-blue-800'
                              : isDark ? 'bg-indigo-500/20 text-indigo-400' : 'bg-indigo-100 text-indigo-800'">
                        <svg class="w-3 h-3 mr-1" fill="currentColor" viewBox="0 0 20 20">
                          <path d="M13 6a3 3 0 11-6 0 3 3 0 016 0zM18 8a2 2 0 11-4 0 2 2 0 014 0zM14 15a4 4 0 00-8 0v3h8v-3zM6 8a2 2 0 11-4 0 2 2 0 014 0zM16 18v-3a5.972 5.972 0 00-.75-2.906A3.005 3.005 0 0119 15v3h-3zM4.75 12.094A5.973 5.973 0 004 15v3H1v-3a3 3 0 013.75-2.906z"/>
                        </svg>
                        {{ blessing.recipientId === 0 ? '发送给所有人' : '发送给我' }}
                      </span>
                    </div>
                  </div>
                </div>

                <!-- 操作按钮（如果是自己的消息） -->
                <div v-if="blessing.userId === currentUser?.id" class="flex space-x-2 ml-4">
                  <button
                    @click="editBlessing(blessing)"
                    class="p-2 rounded-full transition-colors"
                    :class="isDark ? 'bg-white/5 hover:bg-white/10 text-white/50 hover:text-indigo-400' : 'bg-gray-100 hover:bg-gray-200 text-gray-400 hover:text-indigo-600'"
                    title="编辑"
                  >
                    <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"/>
                    </svg>
                  </button>
                  <button
                    @click="confirmDelete(blessing)"
                    class="p-2 rounded-full transition-colors"
                    :class="isDark ? 'bg-white/5 hover:bg-white/10 text-white/50 hover:text-red-400' : 'bg-gray-100 hover:bg-gray-200 text-gray-400 hover:text-red-600'"
                    title="删除"
                  >
                    <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/>
                    </svg>
                  </button>
                </div>
              </div>
            </div>
          </div>
        </template>

        <!-- 空状态 -->
        <div v-else class="text-center py-12">
          <svg class="mx-auto h-12 w-12" :class="isDark ? 'text-white/30' : 'text-gray-300'" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z"/>
          </svg>
          <h3 class="mt-2 text-sm font-medium" :class="isDark ? 'text-white' : 'text-gray-900'">暂无消息</h3>
          <p class="mt-1 text-sm" :class="isDark ? 'text-white/50' : 'text-gray-500'">还没有人给你发送消息。</p>
          <button
            @click="showSendModal = true"
            class="mt-6 inline-flex items-center px-4 py-2 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-indigo-600 hover:bg-indigo-700"
          >
            发送第一条消息
          </button>
        </div>
      </div>

      <!-- 我发送的消息 -->
      <div v-if="activeTab === 'sent'" class="backdrop-blur-md rounded-2xl p-6 border" :class="isDark ? 'bg-gradient-to-br from-white/5 to-white/3 border-white/10' : 'bg-gray-50 border-gray-200'">
        <div class="flex justify-between items-center mb-6">
          <h2 class="text-2xl font-bold" :class="isDark ? 'text-white' : 'text-gray-900'">我发送的消息</h2>
          <button
            @click="loadSentBlessings"
            class="text-sm flex items-center transition-colors"
            :class="isDark ? 'text-indigo-400 hover:text-indigo-300' : 'text-indigo-600 hover:text-indigo-800'"
          >
            <svg class="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"/>
            </svg>
            刷新
          </button>
        </div>

        <!-- 发送的消息列表 -->
        <div v-if="sentBlessings.length > 0" class="space-y-4">
          <div
            v-for="blessing in sentBlessings"
            :key="blessing.id"
            class="border rounded-lg p-4 transition-all hover:scale-[1.01]"
            :class="isDark ? 'bg-white/5 border-white/10 hover:border-indigo-400/30' : 'bg-white border-gray-200 hover:border-indigo-300'"
          >
            <div class="flex justify-between items-start">
              <div class="flex-1">
                <div class="flex justify-between mb-2">
                  <div class="flex items-center">
                    <span class="font-bold" :class="isDark ? 'text-white' : 'text-gray-900'">{{ blessing.content }}</span>
                  </div>
                  <span class="text-sm" :class="isDark ? 'text-white/60' : 'text-gray-500'">{{ formatTime(blessing.time) }}</span>
                </div>

                <div class="flex items-center text-sm" :class="isDark ? 'text-white/60' : 'text-gray-600'">
                  <svg class="w-4 h-4 mr-1" fill="currentColor" viewBox="0 0 20 20">
                    <path fill-rule="evenodd" d="M10 9a3 3 0 100-6 3 3 0 000 6zm-7 9a7 7 0 1114 0H3z" clip-rule="evenodd"/>
                  </svg>
                  发送给：
                  <span class="ml-1 font-medium" :class="blessing.recipientId === 0 
                    ? isDark ? 'text-blue-400' : 'text-blue-600'
                    : isDark ? 'text-indigo-400' : 'text-indigo-600'">
                    {{ blessing.recipientId === 0 ? '所有人' : `用户 ${blessing.recipientId}` }}
                  </span>
                </div>
              </div>

              <!-- 操作按钮 -->
              <div class="flex space-x-2 ml-4">
                <button
                  @click="editBlessing(blessing)"
                  class="p-2 rounded-full transition-colors"
                  :class="isDark ? 'bg-white/5 hover:bg-white/10 text-white/50 hover:text-indigo-400' : 'bg-gray-100 hover:bg-gray-200 text-gray-400 hover:text-indigo-600'"
                  title="编辑"
                >
                  <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"/>
                  </svg>
                </button>
                <button
                  @click="confirmDelete(blessing)"
                  class="p-2 rounded-full transition-colors"
                  :class="isDark ? 'bg-white/5 hover:bg-white/10 text-white/50 hover:text-red-400' : 'bg-gray-100 hover:bg-gray-200 text-gray-400 hover:text-red-600'"
                  title="删除"
                >
                  <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/>
                  </svg>
                </button>
              </div>
            </div>
          </div>
        </div>

        <!-- 空状态 -->
        <div v-else class="text-center py-12">
          <svg class="mx-auto h-12 w-12" :class="isDark ? 'text-white/30' : 'text-gray-300'" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8"/>
          </svg>
          <h3 class="mt-2 text-sm font-medium" :class="isDark ? 'text-white' : 'text-gray-900'">暂无发送记录</h3>
          <p class="mt-1 text-sm" :class="isDark ? 'text-white/50' : 'text-gray-500'">你还没有发送过任何消息。</p>
          <button
            @click="showSendModal = true"
            class="mt-6 inline-flex items-center px-4 py-2 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-indigo-600 hover:bg-indigo-700"
          >
            发送第一条消息
          </button>
        </div>
      </div>
    </div>

    <!-- 发送消息模态框 -->
    <div v-if="showSendModal" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <div class="backdrop-blur-md rounded-2xl p-6 border w-full max-w-2xl max-h-[90vh] overflow-y-auto" :class="isDark ? 'bg-gradient-to-br from-white/5 to-white/3 border-white/10' : 'bg-white border-gray-200'">
        <div class="flex justify-between items-center mb-6">
          <h3 class="text-xl font-bold" :class="isDark ? 'text-white' : 'text-gray-900'">{{ editingBlessing ? '编辑消息' : '发送消息' }}</h3>
          <button @click="closeModal" :class="isDark ? 'text-white/50 hover:text-white' : 'text-gray-400 hover:text-gray-600'">
            <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
            </svg>
          </button>
        </div>

        <form @submit.prevent="sendBlessing" class="space-y-4">
          <!-- 接收对象选择 -->
          <div>
            <label class="block text-sm font-medium mb-2" :class="isDark ? 'text-white/70' : 'text-gray-700'">发送给</label>
            <div class="grid grid-cols-2 gap-3">
              <label class="flex items-center p-3 border rounded-lg cursor-pointer transition-colors"
                     :class="form.sendToAll 
                       ? isDark ? 'border-indigo-500 bg-indigo-500/20' : 'border-indigo-500 bg-indigo-50'
                       : isDark ? 'border-white/10 hover:border-white/20' : 'border-gray-200 hover:border-gray-300'">
                <input
                  type="radio"
                  v-model="form.sendToAll"
                  :value="true"
                  class="mr-2"
                  :class="isDark ? 'text-indigo-400' : 'text-indigo-600'"
                >
                <div>
                  <div class="font-medium" :class="isDark ? 'text-white' : 'text-gray-900'">全体人员</div>
                  <div class="text-xs" :class="isDark ? 'text-white/50' : 'text-gray-500'">发送给所有人</div>
                </div>
              </label>

              <label class="flex items-center p-3 border rounded-lg cursor-pointer transition-colors"
                     :class="!form.sendToAll 
                       ? isDark ? 'border-indigo-500 bg-indigo-500/20' : 'border-indigo-500 bg-indigo-50'
                       : isDark ? 'border-white/10 hover:border-white/20' : 'border-gray-200 hover:border-gray-300'">
                <input
                  type="radio"
                  v-model="form.sendToAll"
                  :value="false"
                  class="mr-2"
                  :class="isDark ? 'text-indigo-400' : 'text-indigo-600'"
                >
                <div>
                  <div class="font-medium" :class="isDark ? 'text-white' : 'text-gray-900'">指定用户</div>
                  <div class="text-xs" :class="isDark ? 'text-white/50' : 'text-gray-500'">选择多个用户</div>
                </div>
              </label>
            </div>
          </div>

          <!-- 多选用户 -->
          <div v-if="!form.sendToAll">
            <label class="block text-sm font-medium mb-2" :class="isDark ? 'text-white/70' : 'text-gray-700'">选择用户</label>
            <div class="mb-2">
              <input
                v-model="userSearchQuery"
                type="text"
                placeholder="搜索用户..."
                class="w-full px-3 py-2 border rounded-lg"
                :class="isDark ? 'bg-white/5 border-white/10 text-white' : 'bg-white border-gray-300 text-gray-900'"
              >
            </div>
            <div class="border rounded-lg max-h-64 overflow-y-auto p-2" :class="isDark ? 'border-white/10 bg-white/5' : 'border-gray-200 bg-gray-50'">
              <div v-if="filteredUsers.length === 0" class="text-center py-4 text-sm" :class="isDark ? 'text-white/50' : 'text-gray-500'">
                暂无用户
              </div>
              <div v-else class="space-y-2">
                <label
                  v-for="user in filteredUsers"
                  :key="user.id"
                  class="flex items-center p-2 rounded-lg cursor-pointer transition-colors"
                  :class="form.selectedUserIds.includes(user.id)
                    ? isDark ? 'bg-indigo-500/20 border border-indigo-500/50' : 'bg-indigo-50 border border-indigo-200'
                    : isDark ? 'hover:bg-white/5' : 'hover:bg-gray-100'"
                >
                  <input
                    type="checkbox"
                    :value="user.id"
                    v-model="form.selectedUserIds"
                    class="mr-3"
                    :class="isDark ? 'text-indigo-400' : 'text-indigo-600'"
                  >
                  <div class="flex items-center flex-1">
                    <img v-if="user.avatar" :src="user.avatar" class="w-8 h-8 rounded-full mr-2" alt="头像">
                    <div class="flex-1">
                      <div class="font-medium" :class="isDark ? 'text-white' : 'text-gray-900'">{{ user.account || user.name || `用户${user.id}` }}</div>
                      <div class="text-xs" :class="isDark ? 'text-white/50' : 'text-gray-500'">ID: {{ user.id }}</div>
                    </div>
                  </div>
                </label>
              </div>
            </div>
            <p class="mt-2 text-xs" :class="isDark ? 'text-white/50' : 'text-gray-500'">
              已选择 {{ form.selectedUserIds.length }} 个用户
            </p>
          </div>

          <!-- 消息内容 -->
          <div>
            <label class="block text-sm font-medium mb-2" :class="isDark ? 'text-white/70' : 'text-gray-700'">消息内容</label>
            <textarea
              v-model="form.content"
              rows="4"
              class="w-full px-3 py-2 border rounded-lg resize-none"
              :class="isDark ? 'bg-white/5 border-white/10 text-white' : 'bg-white border-gray-300 text-gray-900'"
              placeholder="写下你的消息内容..."
              maxlength="255"
              required
            ></textarea>
            <div class="flex justify-between mt-1">
              <span class="text-xs" :class="isDark ? 'text-white/50' : 'text-gray-500'">最大255个字符</span>
              <span class="text-xs" :class="form.content.length > 250 ? 'text-red-600' : (isDark ? 'text-white/50' : 'text-gray-500')">
                {{ form.content.length }}/255
              </span>
            </div>
          </div>

          <!-- 操作按钮 -->
          <div class="flex space-x-3 pt-4">
            <button
              type="button"
              @click="closeModal"
              class="flex-1 py-3 rounded-lg transition-colors"
              :class="isDark ? 'bg-white/5 hover:bg-white/10 border border-white/10 text-white' : 'bg-gray-100 hover:bg-gray-200 text-gray-700'"
            >
              取消
            </button>
            <button
              type="submit"
              :disabled="sending || !form.content.trim() || (!form.sendToAll && form.selectedUserIds.length === 0)"
              class="flex-1 bg-indigo-600 hover:bg-indigo-700 text-white py-3 rounded-lg font-medium focus:outline-none focus:ring-2 focus:ring-indigo-500 disabled:opacity-50 disabled:cursor-not-allowed transition-all"
            >
              <span v-if="!sending">{{ editingBlessing ? '更新消息' : '发送消息' }}</span>
              <span v-else class="flex items-center justify-center">
                <svg class="animate-spin -ml-1 mr-3 h-5 w-5 text-white" fill="none" viewBox="0 0 24 24">
                  <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                  <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                </svg>
                处理中...
              </span>
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- 删除确认模态框 -->
    <div v-if="showDeleteConfirm" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <div class="backdrop-blur-md rounded-2xl p-6 border max-w-sm w-full" :class="isDark ? 'bg-gradient-to-br from-white/5 to-white/3 border-white/10' : 'bg-white border-gray-200'">
        <div class="text-center">
          <svg class="mx-auto h-12 w-12 text-red-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.998-.833-2.732 0L4.732 16.5c-.77.833.192 2.5 1.732 2.5z"/>
          </svg>
          <h3 class="mt-4 text-lg font-medium" :class="isDark ? 'text-white' : 'text-gray-900'">删除消息</h3>
          <p class="mt-2 text-sm" :class="isDark ? 'text-white/50' : 'text-gray-500'">
            确定要删除这条消息吗？删除后无法恢复。
          </p>
        </div>
        <div class="mt-6 flex space-x-3">
          <button
            @click="showDeleteConfirm = false"
            class="flex-1 py-3 rounded-lg transition-colors"
            :class="isDark ? 'bg-white/5 hover:bg-white/10 border border-white/10 text-white' : 'bg-gray-100 hover:bg-gray-200 text-gray-700'"
          >
            取消
          </button>
          <button
            @click="deleteBlessing"
            :disabled="deleting"
            class="flex-1 bg-red-600 text-white py-3 rounded-lg font-medium hover:bg-red-700 focus:outline-none focus:ring-2 focus:ring-red-500 disabled:opacity-50 disabled:cursor-not-allowed transition"
          >
            <span v-if="!deleting">确定删除</span>
            <span v-else class="flex items-center justify-center">
              <svg class="animate-spin -ml-1 mr-3 h-5 w-5 text-white" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
              </svg>
              删除中...
            </span>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch, computed } from 'vue'
import { useRouter } from 'vue-router'
import { get, post, del } from '@/net/index.js'
import { message } from 'ant-design-vue'
import { isDark } from '@/stores/theme.js'

const router = useRouter()
const messageApi = message

// 用户信息
const currentUser = ref({})
const allUsers = ref([])
const userSearchQuery = ref('')

// 数据
const receivedBlessings = ref([])
const sentBlessings = ref([])
const activeTab = ref('received')
// 模态框状态
const showSendModal = ref(false)
const showDeleteConfirm = ref(false)
const sending = ref(false)
const deleting = ref(false)
const editingBlessing = ref(null)
const blessingToDelete = ref(null)

// 表单
const form = reactive({
  sendToAll: true,
  selectedUserIds: [],
  content: ''
})

// 过滤用户列表
const filteredUsers = computed(() => {
  if (!userSearchQuery.value.trim()) {
    return allUsers.value.filter(u => u.id !== currentUser.value.id)
  }
  const query = userSearchQuery.value.toLowerCase().trim()
  return allUsers.value.filter(u => 
    u.id !== currentUser.value.id &&
    (u.account?.toLowerCase().includes(query) ||
     u.name?.toLowerCase().includes(query) ||
     u.id?.toString().includes(query))
  )
})

// 格式化时间
const formatTime = (timeString) => {
  if (!timeString) return ''
  const date = new Date(timeString)
  return date.toLocaleString('zh-CN', {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 从后端API获取当前用户信息
const loadCurrentUser = () => {
  get('/api/user/current', {}, (message, data) => {
    currentUser.value = data
    console.log('当前用户信息:', currentUser.value)
  }, (errorMessage) => {
    console.error('获取用户信息失败:', errorMessage)
    messageApi.error('获取用户信息失败')
  })
}

// 加载所有用户
const loadAllUsers = () => {
  get('/api/user/AllUser', {}, (message, data) => {
    allUsers.value = data || []
    console.log('所有用户:', allUsers.value)
  }, (errorMessage) => {
    console.error('加载用户列表失败:', errorMessage)
    allUsers.value = []
  })
}

// 加载收到的消息
const loadReceivedBlessings = () => {
  get('/api/wall/getWallByRecipientId', null, (message, data) => {
    console.log('收到的消息:', data)
    receivedBlessings.value = data || []
  }, (errorMessage) => {
    console.error('加载收到的消息失败:', errorMessage)
    receivedBlessings.value = []
    messageApi.error('加载收到的消息失败')
  })
}

// 加载发送的消息
const loadSentBlessings = () => {
  get('/api/wall/getWallByUserId', null, (message, data) => {
    console.log('发送的消息:', data)
    sentBlessings.value = data || []
  }, (errorMessage) => {
    console.error('加载发送的消息失败:', errorMessage)
    sentBlessings.value = []
    messageApi.error('加载发送的消息失败')
  })
}

// 发送/更新消息
const sendBlessing = async () => {
  if (!form.content.trim()) {
    messageApi.error('请输入消息内容')
    return
  }

  if (!form.sendToAll && form.selectedUserIds.length === 0) {
    messageApi.error('请至少选择一个接收用户')
    return
  }

  sending.value = true

  try {
    if (editingBlessing.value) {
      // 更新消息（编辑时只能更新内容，不能改变接收者）
      const updateData = {
        content: form.content,
        id: editingBlessing.value.id
      }

      post('/api/wall/updateWall', updateData, (message, data) => {
        messageApi.success(message || '更新成功')
        closeModal()
        if (activeTab.value === 'received') {
          loadReceivedBlessings()
        } else {
          loadSentBlessings()
        }
      }, (errorMessage) => {
        messageApi.error(errorMessage || '更新失败')
      }, () => {
        sending.value = false
      })
    } else {
      // 发送新消息
      const recipients = form.sendToAll ? [0] : form.selectedUserIds
      let successCount = 0
      let failCount = 0

      // 批量发送给多个用户
      for (const recipientId of recipients) {
        const blessingData = {
          userId: currentUser.value.id,
          recipientId: recipientId,
          content: form.content,
          userName: currentUser.value.account,
          userAvatar: currentUser.value.avatar
        }

        await new Promise((resolve) => {
          post('/api/wall/addWall', blessingData, (message) => {
            successCount++
            resolve()
          }, (errorMessage) => {
            failCount++
            console.error('发送失败:', errorMessage)
            resolve()
          })
        })
      }

      if (successCount > 0) {
        messageApi.success(`成功发送 ${successCount} 条消息${failCount > 0 ? `，${failCount} 条失败` : ''}`)
        closeModal()
        if (activeTab.value === 'received') {
          loadReceivedBlessings()
        } else {
          loadSentBlessings()
        }
      } else {
        messageApi.error('发送失败，请重试')
      }
      sending.value = false
    }
  } catch (error) {
    console.error('发送消息异常:', error)
    messageApi.error('发送失败，请重试')
    sending.value = false
  }
}

// 编辑消息
const editBlessing = (blessing) => {
  editingBlessing.value = blessing
  form.sendToAll = blessing.recipientId === 0
  form.selectedUserIds = blessing.recipientId === 0 ? [] : [blessing.recipientId]
  form.content = blessing.content
  showSendModal.value = true
}

// 确认删除
const confirmDelete = (blessing) => {
  blessingToDelete.value = blessing
  showDeleteConfirm.value = true
}

// 删除消息
const deleteBlessing = () => {
  if (!blessingToDelete.value) return

  deleting.value = true

  const deleteData = {
    userId: currentUser.value.id,
    id: blessingToDelete.value.id
  }

  del('/api/wall/deleteWall', deleteData, (message) => {
    messageApi.success(message || '删除成功')
    if (activeTab.value === 'received') {
      loadReceivedBlessings()
    } else {
      loadSentBlessings()
    }
  }, (errorMessage) => {
    messageApi.error(errorMessage || '删除失败')
  }, () => {
    deleting.value = false
    showDeleteConfirm.value = false
    blessingToDelete.value = null
  })
}

// 关闭模态框
const closeModal = () => {
  showSendModal.value = false
  showDeleteConfirm.value = false
  editingBlessing.value = null
  form.sendToAll = true
  form.selectedUserIds = []
  form.content = ''
  userSearchQuery.value = ''
}

// 退出登录
const handleLogout = () => {
  localStorage.clear()
  router.push('/')
}

// 初始化加载
onMounted(() => {
  console.log('Wall页面初始化')

  // 加载用户信息
  loadCurrentUser()

  // 加载用户列表
  loadAllUsers()

  // 加载消息数据
  loadReceivedBlessings()

  console.log('初始化完成，用户信息:', currentUser.value)
})

// 监听标签页切换，自动加载对应数据
watch(() => activeTab.value, (newTab) => {
  if (newTab === 'received') {
    loadReceivedBlessings()
  } else if (newTab === 'sent') {
    loadSentBlessings()
  }
})
</script>

<style scoped>
/* 自定义滚动条 */
.overflow-y-auto::-webkit-scrollbar {
  width: 6px;
}

.overflow-y-auto::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.05);
  border-radius: 3px;
}

.overflow-y-auto::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.2);
  border-radius: 3px;
}

.overflow-y-auto::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.3);
}
</style>
const recipientId = ref(0)
