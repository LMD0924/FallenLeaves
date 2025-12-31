/*
import axios from "axios";
import {ElMessage} from "element-plus";//引入用到的组件

function getAuthToken() {
  return localStorage.getItem('authToken') || '';
}

const defaultError = () => ElMessage.error('发生错误，请联系管理员。') //定义默认错误提示语
const defaultFailure = (message) => ElMessage.warning(message) //后端请求返回失败信息时将其打印

// 将对象转换为URLSearchParams格式
function objectToURLSearchParams(obj) {
  const params = new URLSearchParams();
  for (const key in obj) {
    if (obj.hasOwnProperty(key)) {
      params.append(key, obj[key]);
    }
  }
  return params;
}

//post请求示例
function post(url, data, success, failure = defaultFailure, error = defaultError) {//导入请求路径url,请求数据data,以及失败和成功的操作
  axios.post(url, data, { //使用axios的post请求 传入路径和数据
    headers: {
      "Content-Type": "application/x-www-form-urlencoded", //设置内容类型
      "Authorization": getAuthToken()
    },
    withCredentials: true
  }).then(({data}) => {
    if (data.success)
      success(data.message, data.data,data.status) //判断数据内含的请求成功或失败并做出对应前端操作，执行的操作在组件中引用时书写
    else
      failure(data.message,data.data, data.status)
  }).catch(error)
}

function get(url, data = null, success, failure = defaultFailure, error = defaultError) {
  const config = {
    withCredentials: true,
    params: data , // 将数据作为查询参数
    headers:{
      "Authorization": getAuthToken()
    }
  };

  axios.get(url, config)
    .then(({data}) => {
      if (data.success)
        success(data.message,data.data, data.status)
      else
        failure(data.message,data.data, data.status)
    })
    .catch(error)
}

function put(url, data, success, failure = defaultFailure, error = defaultError) {
  const params = new URLSearchParams();
  for (const key in data) {
    params.append(key, data[key]);
  }

  axios.put(url, params, {
    headers: {
      "Content-Type": "application/x-www-form-urlencoded",
      "Authorization": getAuthToken(),
    },
    withCredentials: true,
  })
    .then(({ data }) => {
      if (data.success) {
        success(data.message, data.data);
      } else {
        failure(data.message);
      }
    })
    .catch(error);
}

function del(url, data, success, failure = defaultFailure, error = defaultError) {
  const config = {
    withCredentials: true,
    params: data,
    headers: {
      "Authorization": getAuthToken(),
    },
  };

  axios.delete(url, config)
    .then(({ data }) => {
      if (data.success) {
        success(data.message, data.data);
      } else {
        failure(data.message);
      }
    })
    .catch(error);
}
export { get, post, put, del }; //导出get post InternalGet方法 供所有组件使用



*/
import axios from "axios";
import {ElMessage} from "element-plus";

function getAuthToken() {
  return localStorage.getItem('authToken') || '';
}

const defaultError = () => ElMessage.error('发生错误，请联系管理员。')
const defaultFailure = (message) => ElMessage.warning(message)

// 根据 RestBean 的类型显示不同的消息
function handleResponse(data, successCallback, failureCallback) {
  if (data.success) {
    // 成功：success=true
    if (data.type === 'success') {
      ElMessage.success(data.message || '操作成功');
    } else if (data.type === 'info') {
      ElMessage.info(data.message || '提示信息');
    }
    successCallback && successCallback(data.message, data.data, data.code, data.type);
  } else {
    // 失败：success=false
    if (data.type === 'warning') {
      ElMessage.warning(data.message || '操作失败');
    } else if (data.type === 'error') {
      ElMessage.error(data.message || '系统错误');
    }
    failureCallback && failureCallback(data.message, data.data, data.code, data.type);
  }
}

// 对象转URL参数
function objectToURLSearchParams(obj) {
  const params = new URLSearchParams();
  for (const key in obj) {
    if (obj.hasOwnProperty(key)) {
      params.append(key, obj[key]);
    }
  }
  return params;
}

// POST 请求
function post(url, data, success, failure = defaultFailure, error = defaultError) {
  axios.post(url, objectToURLSearchParams(data), {
    headers: {
      "Content-Type": "application/x-www-form-urlencoded",
      "Authorization": getAuthToken()
    },
    withCredentials: true
  }).then(({data}) => {
    handleResponse(data, success, failure);
  }).catch(err => {
    console.error('请求错误:', err);
    error();
  })
}

// GET 请求
function get(url, data = null, success, failure = defaultFailure, error = defaultError) {
  const config = {
    withCredentials: true,
    params: data,
    headers: {
      "Authorization": getAuthToken()
    }
  };

  axios.get(url, config)
    .then(({data}) => {
      handleResponse(data, success, failure);
    })
    .catch(err => {
      console.error('请求错误:', err);
      error();
    })
}

// PUT 请求
function put(url, data, success, failure = defaultFailure, error = defaultError) {
  axios.put(url, objectToURLSearchParams(data), {
    headers: {
      "Content-Type": "application/x-www-form-urlencoded",
      "Authorization": getAuthToken(),
    },
    withCredentials: true,
  })
    .then(({ data }) => {
      handleResponse(data, success, failure);
    })
    .catch(err => {
      console.error('请求错误:', err);
      error();
    });
}

// DELETE 请求
function del(url, data, success, failure = defaultFailure, error = defaultError) {
  const config = {
    withCredentials: true,
    params: data,
    headers: {
      "Authorization": getAuthToken(),
    },
  };

  axios.delete(url, config)
    .then(({ data }) => {
      handleResponse(data, success, failure);
    })
    .catch(err => {
      console.error('请求错误:', err);
      error();
    });
}

// 上传文件
function upload(url, formData, success, failure = defaultFailure, error = defaultError) {
  axios.post(url, formData, {
    headers: {
      "Content-Type": "multipart/form-data",
      "Authorization": getAuthToken()
    },
    withCredentials: true
  }).then(({data}) => {
    handleResponse(data, success, failure);
  }).catch(err => {
    console.error('上传错误:', err);
    error();
  })
}

export { get, post, put, del, upload };
