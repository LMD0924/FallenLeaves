import { createRouter, createWebHistory } from 'vue-router'
import {get} from "@/net/index.js";
import {userUserStore} from "@/stores/userStore.js";

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'auth',
      component: () => import('@/components/Auth/ExamLogin.vue'),
    },
    {
      path:'/ExamSidebar',
      name:'考试侧边栏组件',
      component:()=>import('@/components/ExamSidebar.vue'),
      children:[
        {
          path:'/ExamSidebar',
          name:'考试首页',
          component:()=>import('@/views/exam/Exam_Home.vue')
        },
        {
          path:'/CreateExam',
          name:'创建考试',
          component:()=>import('@/views/exam/CreateExam.vue')
        },
        {
          path:'/UserManage',
          name:'用户管理',
          component:()=>import('@/views/exam/UserManage.vue')
        },
        {
          path:'/ClassManage',
          name:'班级管理',
          component:()=>import('@/views/exam/ClassManage.vue')
        },
        {
          path:'/Exam',
          name:'考试',
          component:()=>import('@/views/exam/Exam.vue')
        },
        {
          path:"/ExamUser",
          name:"考试个人信息",
          component:()=>import('@/views/exam/ExamUser.vue')
        },
        {
          path:"/CourseManage",
          name:"课程管理",
          component:()=>import('@/views/exam/CourseManage.vue')
        },
        {
          path:"/QuestionType",
          name:"题型信息",
          component:()=>import('@/views/exam/QuestionType.vue')
        },
        {
          path:"/ExamLeave",
          name:"请假",
          component:()=>import('@/views/exam/ExamLeave.vue')
        },
        {
          path:"/Notice",
          name:"通知",
          component:()=>import('@/views/exam/Notice.vue')
        },
        {
          path:"/NoticeList",
          name:"通知列表",
          component:()=>import('@/views/exam/NoticeList.vue')
        },
        {
          path:"/ExamStats",
          name:"教师端 - 考试统计与批改页面",
          component:()=>import('@/views/exam/ExamStats.vue')
        },
        {
          path:"/GradeCenter",
          name:"学生端 - 个人成绩中心",
          component:()=>import('@/views/exam/GradeCenter.vue')
        },
      ]
    },
  ],
})
//修改网页名
router.beforeEach((to, from, next)=>
{
  const userStore = userUserStore()
  const publicMap = new Map()
  publicMap.set('/', 1)
//  publicMap.set('/share', 2)
  publicMap.set('/error/401', 3)
  publicMap.set('/error/404', 4)
  // 检查要访问的路径是否是根路径

  if (to.matched.length === 0) next('error/404')
  // 检查是否是分享页面
  // if (to.path.startsWith('/share/')) {
  //   next();
  //   return;
  // }
  if (!publicMap.has(to.path)) {
    // 不是访问根路径，检查用户状态
    const user = userStore.user; // 假设你的用户状态保存在Vuex的`user`状态中
    if (user === null) {
      get('api/user/information', {},
        (message, data) => {
          userStore.login(data);
          next();
        }, (message, data) => {
          next('/')
        }, (message, data) => {
          next('/')
        }
      )
    } else {
      // 用户已登录，允许路由继续
      next();
    }
  }
  else {
    // 访问的是根路径或其他公开路径，直接放行
    next();
  }
})
export default router
