package org.example.examback.service.Impl;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.example.examback.entity.*;
import org.example.examback.mapper.ExamMapper;
import org.example.examback.mapper.UserMapper;
import org.example.examback.service.ExamService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
@Slf4j
@Service
public class ExamServiceImpl implements ExamService {
    @Resource
    ExamMapper examMapper;
    @Resource
    UserMapper userMapper;
    //登陆注册
    @Override
    @Transactional
    public int InsertUser(String account,String username,String password,String role,String status){
        try {
            //先插入User表，获取自动生成的id
            User user = new User();
            user.setAccount(account);
            user.setUsername(username);
            user.setPassword(password);
            user.setRole(role);
            user.setStatus(status);
            
            int result = examMapper.InsertUser(user);
            if (result != 1) throw new RuntimeException("插入User表失败");
            
            // 获取自动生成的id
            Integer userId = user.getId();
            if (userId == null) throw new RuntimeException("获取用户ID失败");
            
            //再插入对应角色表
            result = examMapper.InsertXm(account, username, password, role, status, userId);
            if (result != 1) throw new RuntimeException("插入对应表失败");
            return 1;
      }catch(Exception e){
          log.error("注册失败：",e);
          throw e;
      }
    }
    @Override
    public User ExamLogin(String username,String password,String role){
        return examMapper.ExamLogin(username, password, role);
    }
    //根据id查询信息
    @Override
    public User SelectById(Integer id){
        return examMapper.SelectById(id);
    }
    //获取所有用户
    @Override
    public List<User> AllUser(){
        return examMapper.AllUser();
    }
    //获取所有教师
    @Override
    public List<User> AllTeacher(){
        return examMapper.AllTeacher();
    }
    //获取所有学生
    @Override
    public List<User> AllStudent(){
        return examMapper.AllStudent();
    }
    //更新信息
    @Override
    @Transactional
    public int UpdateUserInfo(User user){
        try{
            if("**不给看**".equals(user.getPassword())) {
                user.setPassword(SelectById(user.getId()).getPassword());
            }
            //先更新User表
            int result=examMapper.UpdateUserInfo(user);
            if(result==0) throw new RuntimeException("更新user表失败");
            //在更新对应角色表
            result=examMapper.UpdateXm(user);
            if(result==0) throw new RuntimeException("更新对应角色表失败");
            return 1;
        }catch(Exception e){
            throw new RuntimeException("更新失败",e.getCause());
        }
    }
    //选择专业
    @Override
    public int UpdateUserProfessional(String professional,Integer id){
        return examMapper.UpdateUserProfessional(professional,id);
    }
    //更新头像
    @Override
    @Transactional
    public int UpdateUserAvatar(String avatar,Integer id){
        try{
            // 先获取用户信息以获取角色
            User user = examMapper.SelectById(id);
            if (user == null) {
                throw new RuntimeException("用户不存在");
            }
            
            int result=examMapper.UpdateUserAvatar(avatar,id);
            if(result==0) throw new RuntimeException("更新user表失败");
            
            result=examMapper.UpdateXmAvatar(avatar,id,user.getRole());
            if(result==0) throw new RuntimeException("更新对应角色表失败");
            return 1;
        }catch(Exception e){
            throw new RuntimeException("更新失败",e.getCause());
        }
    }
//---------------------------------有关课程---------------------------
     //添加课程
    @Override
    public int InsertCourse(Course course){
        return examMapper.InsertCourse(course);
    }
    //加入课程
    @Override
    public int InsertCourseUser(Integer course_id,Integer user_id,String role,String account,String statue){
        return examMapper.InsertCourseUser(course_id,user_id,role,account,statue);
    }
    //查询课程用户
    @Override
    public List<Course> SelectCourseUser(Integer course_id){
        return examMapper.SelectCourseUser(course_id);
    }
    //查询用户课程
    @Override
    public List<Course> SelectUserCourse(Integer user_id){
        return examMapper.SelectUserCourse(user_id);
    }
    //退出课程
    @Override
    public int DeleteCourseUser(Integer user_id,Integer course_id){
        return examMapper.DeleteCourseUser(user_id,course_id);
    }
    //获取所有课程
    @Override
    public List<Course> AllCourse(){
        return examMapper.AllCourse();
    }
    //根据id查询课程
    @Override
    public Course SelectCourseById(Integer id){
        return examMapper.SelectCourseById(id);
    }
    //根据教师id查询课程
    @Override
    public List<Course> SelectCourseByTeacherId(Integer teacher_id){
        return examMapper.SelectCourseByTeacherId(teacher_id);
    }
    //根据id修改课程
    public int UpdateCourse(Course course){
        return examMapper.UpdateCourse(course);
    }
//--------------------------有关题目---------------------------------
    //添加题目
    @Override
    public int InsertQuestion(QuestionType questiontype){
        return examMapper.InsertQuestion(questiontype);
    }
    //查询所有题目
    @Override
    public List<QuestionType> AllQuestion(){
        return examMapper.AllQuestion();
    }
    //根据题目id查询
    @Override
    public QuestionType SelectQuestionById(Integer id){
        return examMapper.SelectQuestionById(id);
    }
    //根据课程id查询
    @Override
    public List<QuestionType> SelectQuestionByCourseId(Integer course_id){
        return examMapper.SelectQuestionByCourseId(course_id);
    }
    //根据教师id查询
    @Override
    public List<QuestionType> SelectQuestionByTeacherId(Integer teacher_id){
        return examMapper.SelectQuestionByTeacherId(teacher_id);
    }
    //教师修改题目
    @Override
    public int UpdateQuestion(QuestionType questiontype){
        return examMapper.UpdateQuestion(questiontype);
    }
    //管理员修改题目状态
    @Override
    public int UpdateQuestionStatus(String status,Integer id){
        return examMapper.UpdateQuestionStatus(status,id);
    }
    //----------------------有关班级--------------------------------------
    //添加班级
    @Override
    public int InsertClass(String name,Integer teacher_id,String introduction,String status,String major,String grade){
        return examMapper.InsertClass(name,teacher_id,introduction,status,major,grade);
    }
    //修改班级
    @Override
    public int UpdateClass(ExamClass examclass){
        return examMapper.UpdateClass(examclass);
    }
    //删除班级
    @Override
    public int DeleteClass(Integer id){
        return examMapper.DeleteClass(id);
    }
    //查询所有班级
    @Override
    public List<ExamClass> AllClass(){
        return examMapper.AllClass();
    }
    //用户加入班级
    @Override
    @Transactional
    public int JoinClass(Integer class_id,Integer id){
        try{
            //先更新班级表
            int result=examMapper.UpdateClassSize(class_id);
            if(result==0) throw new RuntimeException("更新班级表失败");
            //在更新用户表
            result=examMapper.JoinClass(class_id,id);
            if(result==0) throw new RuntimeException("更新用户表失败");
            return 1;
        }catch(Exception e){
            throw new RuntimeException("加入班级失败",e.getCause());
        }
    }
    //退出班级
    @Override
    @Transactional
    public int ExitClass(Integer class_id,Integer id){
        try{
            //先更新班级表
            int result=examMapper.UpdateClassSizeExit(class_id);
            if(result==0) throw new RuntimeException("更新班级表失败");
            //在更新用户表
            result=examMapper.ExitClass(id);
            if(result==0) throw new RuntimeException("更新用户表失败");
            return 1;
        }catch(Exception e){
            throw new RuntimeException("退出班级失败",e.getCause());
        }
    }
    //根据id查询班级
    @Override
    public ExamClass SelectClassById(Integer id){return examMapper.SelectClassById(id);}
    //---------------------有关试卷--------------------------------------------------
    //添加试卷
    @Override
    public int InsertExam(Exam exam){
        return examMapper.InsertExam(exam);
    }
    //修改试卷
    @Override
    public int UpdateExam(Exam exam){
        return examMapper.UpdateExam(exam);
    }
    //查询所有试卷
    @Override
    public List<Exam> AllExam(){
        return examMapper.AllExam();
    }
    //根据试卷id查询
    @Override
    public Exam SelectExamById(Integer id){
        return examMapper.SelectExamById(id);
    }
    //根据课程id查询试卷
    @Override
    public List<Exam> SelectExamByCourseId(Integer course_id){
        return examMapper.SelectExamByCourseId(course_id);
    }
    //根据教师id查询试卷
    @Override
    public List<Exam> SelectExamByTeacherId(Integer teacher_id){
        return examMapper.SelectExamByTeacherId(teacher_id);
    }
    //删除试卷
    @Override
    public int DeleteExam(Integer id){
        return examMapper.DeleteExam(id);
    }
    //添加试卷题目
    @Override
    public int InsertExamQuestion(Integer exam_id,Integer question_id){
        return examMapper.InsertExamQuestion(exam_id,question_id);
    }
    //根据试卷id查询题目
    @Override
    public List<ExamQuestionVO> getExamQuestionsByExamId(Integer exam_id){
        return examMapper.getExamQuestionsByExamId(exam_id);
    }
    //根据试卷id删除题目
    @Override
    public Integer DeleteExamQuestion(Integer exam_id,Integer question_id){
        return examMapper.DeleteExamQuestion(exam_id,question_id);
    }
    //------------------------有关请假------------------------------------------
    //添加请假记录
    @Override
    public Integer addVacation(Vacation vacation){
        return examMapper.addVacation(vacation);
    }
    //获取请假记录
    @Override
    public List<Vacation> getAllVacation(){
        return examMapper.getAllVacation();
    }
    @Override
    public List<Vacation> getVacation(Integer userId){
        return examMapper.getVacation(userId);
    }
    //审批
    @Override
    public Integer updateVacation(Integer id,String status){
        return examMapper.updateVacation(id,status);
    }
}

