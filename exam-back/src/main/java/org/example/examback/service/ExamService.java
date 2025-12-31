package org.example.examback.service;


import org.example.examback.entity.*;

import java.util.List;

public interface ExamService {
    //登录注册
    int InsertUser(String account,String username,String password,String role,String status);
    User ExamLogin(String username, String password, String role);
    //根据id查询信息
    User SelectById(Integer id);
    //获取所有用户
    List<User> AllUser();
    //获取所有教师
    List<User> AllTeacher();
    //获取所有学生
    List<User> AllStudent();
    //更新信息
    int UpdateUserInfo(User user);
    //选择专业
    int UpdateUserProfessional(String professional,Integer id);
    //更新头像
    int UpdateUserAvatar(String avatar,Integer id);
//----------------------有关课程--------------------------------------
  //添加课程
  int InsertCourse(Course course);
  //加入课程
    int InsertCourseUser(Integer course_id,Integer user_id,String role,String account,String statue);
    //查询课程用户
    List<Course> SelectCourseUser(Integer course_id);
    //查询用户课程
    List<Course> SelectUserCourse(Integer user_id);
    //退出课程
    int DeleteCourseUser(Integer user_id,Integer course_id);
  //获取所有课程
    List<Course> AllCourse();
   //根据id查询课程
   Course SelectCourseById(Integer id);
   //根据教师id查询课程
    List<Course> SelectCourseByTeacherId(Integer teacher_id);
    //根据课程id修改
    int UpdateCourse(Course course);
//----------------------有关题目----------------------------------------
    //添加题目
    int InsertQuestion(QuestionType questiontype);
    //查询所有题目
    List<QuestionType> AllQuestion();
    //根据题目id查询
    QuestionType SelectQuestionById(Integer id);
    //根据课程id查询
    List<QuestionType> SelectQuestionByCourseId(Integer course_id);
    //根据教师id查询
    List<QuestionType> SelectQuestionByTeacherId(Integer teacher_id);
    //教师修改题目
    int UpdateQuestion(QuestionType questiontype);
    //管理员修改状态
    int UpdateQuestionStatus(String status,Integer id);
    //----------------------有关班级--------------------------------------
    //添加班级
    int InsertClass(String name,Integer teacher_id,String introduction,String status,String major,String grade);
    //修改班级
    int UpdateClass(ExamClass examclass);
    //删除班级
    int DeleteClass(Integer id);
    //查询所有班级
    List<ExamClass> AllClass();
    //用户加入班级
    int JoinClass(Integer class_id,Integer id);
    //退出班级
    int ExitClass(Integer class_id,Integer id);
    //根据id查询班级
    ExamClass SelectClassById(Integer id);
    //---------------------有关试卷--------------------------------------------------
    //添加试卷
    int InsertExam(Exam exam);
    //修改试卷
    int UpdateExam(Exam exam);
    //查询所有试卷
    List<Exam> AllExam();
    //根据试卷id查询
    Exam SelectExamById(Integer id);
    //根据课程id查询试卷
    List<Exam> SelectExamByCourseId(Integer course_id);
    //根据教师id查询试卷
    List<Exam> SelectExamByTeacherId(Integer teacher_id);
    //删除试卷
    int DeleteExam(Integer id);
    //添加试卷题目
    int InsertExamQuestion(Integer exam_id,Integer question_id);
    //根据试卷id查询题目
    List<ExamQuestionVO> getExamQuestionsByExamId(Integer exam_id);
    //根据试卷id删除题目
    Integer DeleteExamQuestion(Integer exam_id, Integer question_id);
    //------------------------------有关请假----------------------------------------------------
    //添加请假记录
    Integer addVacation(Vacation vacation);
    //获取请假记录
    List<Vacation> getAllVacation();
    List<Vacation> getVacation(Integer userId);
    //审批
    Integer updateVacation(Integer id,String status);
}
