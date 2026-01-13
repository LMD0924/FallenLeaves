package org.example.examback.mapper;
import org.example.examback.entity.User;
import org.example.examback.entity.*;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ExamMapper {
 //----------------------有关用户--------------------------
    //登录注册
   @Insert("insert into user(account,username,password,role,status) values(#{account},#{username},#{password},#{role},#{status})")
   @Options(useGeneratedKeys = true, keyProperty = "id")
    int InsertUser(User user);

    @Insert("<script>" +
            "INSERT INTO " +
            "<if test='role == \"管理员\"'>xm_admin</if>" +
            "<if test='role == \"教师\"'>xm_teacher</if>" +
            "<if test='role == \"学生\"'>xm_student</if>" +
            " (user_id, account, username, password, role, status) " +
            "VALUES (#{user_id}, #{account}, #{username}, #{password}, #{role}, #{status})" +
            "</script>")
    int InsertXm(String account, String username, String password, String role, String status, Integer user_id);

    //登录
    @Select("select * from user where username=#{username} and password=#{password} and role=#{role} and status='审核通过'")
    User ExamLogin(String username, String password, String role);
    //根据id查询信息
    @Select("select * from user where id=#{id}")
    User SelectById(Integer id);
    //获取所有用户
    @Select("select * from user")
    List<User> AllUser();
    //获取所有教师
    @Select("select * from xm_teacher")
    List<User> AllTeacher();
    //获取所有学生
    @Select("select * from xm_student")
    List<User> AllStudent();
    //更新信息
    @Update("update user set account=#{account},username=#{username},password=#{password},role=#{role},status=#{status},phone=#{phone},email=#{email},sex=#{sex},locality=#{locality},general=#{general},professional=#{professional} where id=#{id}")
    int UpdateUserInfo(User user);
 @Update("<script>" +
         "UPDATE " +
         "<choose>" +
         "  <when test='role == &quot;管理员&quot;'>xm_admin</when>" +
         "  <when test='role == &quot;教师&quot;'>xm_teacher</when>" +
         "  <when test='role == &quot;学生&quot;'>xm_student</when>" +
         "</choose> " +
         "SET account=#{account}, " +
         "username=#{username}, " +
         "password=#{password}, " +
         "role=#{role}, " +
         "status=#{status}, " +
         "phone=#{phone}, " +
         "email=#{email}, " +
         "professional=#{professional} " +
         "WHERE user_id=#{user_id}" +
         "</script>")
 int UpdateXm(User user);
 //选择专业
 @Update("update user set professional=#{professional} where id=#{id}")
 int UpdateUserProfessional(String professional,Integer id);
//更新头像
 @Update("update user set avatar=#{avatar} where id=#{id}")
 int UpdateUserAvatar(String avatar,Integer id);
 @Update("<script>" +
         "update " +
         "<choose>" +
         "<when test='role == &quot;管理员&quot;'>xm_admin</when>" +
         "<when test='role == &quot;教师&quot;'>xm_teacher</when>" +
         "<when test='role == &quot;学生&quot;'>xm_student</when>" +
         "</choose>" +
         " set avatar=#{avatar} where user_id=#{user_id}" +
         "</script>")
 int UpdateXmAvatar(String avatar, Integer user_id, String role);
 //-------------------有关课程------------------------------------------------------
//添加课程
 @Insert("insert into xm_course (teacher_id,name,score,status,teacher_name) values(#{teacher_id},#{name},#{score},#{status},#{teacher_name})")
 int InsertCourse(Course course);
 //加入课程
 @Insert("insert into xm_course_user (course_id,user_id,role,account,statue) values(#{course_id},#{user_id},#{role},#{account},#{statue})")
 int InsertCourseUser(Integer course_id,Integer user_id,String role,String account,String statue);
 //查询课程用户
 @Select("select * from xm_course_user where course_id=#{course_id}")
 List<Course> SelectCourseUser(Integer course_id);
 //查询用户课程
 @Select("select cu.*,c.*,user.* from xm_course_user cu,xm_course c,user where cu.course_id=c.id and user_id=#{user_id} and user.id=c.teacher_id")
 List<Course> SelectUserCourse(Integer user_id);
 //退出课程
 @Delete("delete from xm_course_user where user_id=#{user_id} and course_id=#{course_id}")
 int DeleteCourseUser(Integer user_id,Integer course_id);
 // 查询所有课程并关联教师信息
 @Select("SELECT c.*, COALESCE(c.teacher_name, t.account) as teacher_name, t.phone as teacher_phone, t.email as teacher_email " +
         "FROM xm_course c " +
         "LEFT JOIN xm_teacher t ON c.teacher_id = t.user_id")
 List<Course> AllCourse();

 // 根据id查询课程并关联教师信息
 @Select("SELECT c.*, COALESCE(c.teacher_name, t.account) as teacher_name, t.phone as teacher_phone, t.email as teacher_email " +
         "FROM xm_course c " +
         "LEFT JOIN xm_teacher t ON c.teacher_id = t.user_id " +
         "WHERE c.id=#{id}")
 Course SelectCourseById(Integer id);

 // 根据教师id查询课程并关联教师信息
 @Select("SELECT c.*, COALESCE(c.teacher_name, t.account) as teacher_name, t.phone as teacher_phone, t.email as teacher_email " +
         "FROM xm_course c " +
         "LEFT JOIN xm_teacher t ON c.teacher_id = t.user_id " +
         "WHERE c.teacher_id=#{teacher_id}")
 List<Course> SelectCourseByTeacherId(Integer teacher_id);
 //根据课程id修改
 @Update("update xm_course set name=#{name},score=#{score},status=#{status} where id=#{id}")
 int UpdateCourse(Course course);
 //----------------------有关题目--------------------------------------------------

 //添加题目
 @Insert("insert into xm_question_type" +
         "(teacher_id,course_id,questionType,text,score,optionA,optionB,optionC,optionD,optionE,optionF,optionG,answer,knowledge,difficulty,status)" +
         "values(#{teacher_id},#{course_id},#{questionType},#{text},#{score},#{optionA},#{optionB},#{optionC},#{optionD},#{optionE},#{optionF},#{optionG},#{answer},#{knowledge},#{difficulty},#{status})")
  int InsertQuestion(QuestionType questiontype);
 //查询所有题目
 @Select("select * from xm_question_type")
 List<QuestionType> AllQuestion();
 //根据题目id查询
 @Select("select * from xm_question_type where id=#{id}")
 QuestionType SelectQuestionById(Integer id);
 //根据课程id查询题目
 @Select("select * from xm_question_type where course_id=#{course_id}")
 List<QuestionType> SelectQuestionByCourseId(Integer course_id);
 //根据教师id查询题目
 @Select("select * from xm_question_type where teacher_id=#{teacher_id}")
 List<QuestionType> SelectQuestionByTeacherId(Integer teacher_id);
 //教师修改题目
 @Update("update xm_question_type set questionType=#{questionType},text=#{text},score=#{score},optionA=#{optionA},optionB=#{optionB},optionC=#{optionC},optionD=#{optionD},optionE=#{optionE},optionF=#{optionF},optionG=#{optionG},answer=#{answer},knowledge=#{knowledge},difficulty=#{difficulty} where id=#{id}")
 int UpdateQuestion(QuestionType questiontype);
 //管理员修改状态
 @Update("update xm_question_type set status=#{status} where id=#{id}")
 int UpdateQuestionStatus(String status,Integer id);
 //-----------------------有关班级--------------------------------------------------
 //添加班级
 @Insert("insert into xm_class (name,teacher_id,introduction,status,major,grade) values (#{name},#{teacher_id},#{introduction},#{status},#{major},#{grade})")
 int InsertClass(String name,Integer teacher_id,String introduction,String status,String major,String grade);
 //修改班级
 @Update("update xm_class set name=#{name},teacher_id=#{teacher_id},introduction=#{introduction},status=#{status},major=#{major},grade=#{grade} where id=#{id}")
 int UpdateClass(ExamClass examclass);
 //删除班级
 @Delete("delete from xm_class where id=#{id}")
 int DeleteClass(Integer id);
 //查询所有班级
 @Select("select * from xm_class")
 List<ExamClass> AllClass();
 //用户加入班级
 @Update("update user set class_id=#{class_id} where id=#{id}")
 int JoinClass(Integer class_id,Integer id);
@Update("update xm_class set size=COALESCE(size, 0)+1 where id=#{id}")
 int UpdateClassSize(Integer id);
 //退出班级
 @Update("update user set class_id=null where id=#{id}")
 int ExitClass(Integer id);
@Update("update xm_class set size=COALESCE(size, 0)-1 where id=#{id}")
int UpdateClassSizeExit(Integer id);
 //根据id查询班级
    @Select("select * from xm_class where id=#{id}")
 ExamClass SelectClassById(Integer id);
 //---------------------有关试卷--------------------------------------------------
 //添加试卷
 @Insert("insert into xm_exam (title,description,course_id,teacher_id,total_score,duration,start_time,end_time,pass_score,status) values(#{title},#{description},#{course_id},#{teacher_id},#{total_score},#{duration},#{start_time},#{end_time},#{pass_score},#{status})")
 int InsertExam(Exam exam);
 //修改试卷
 @Update("update xm_exam set title=#{title},description=#{description},course_id=#{course_id},teacher_id=#{teacher_id},total_score=#{total_score},duration=#{duration},start_time=#{start_time},end_time=#{end_time},pass_score=#{pass_score},status=#{status} where id=#{id}")
 int UpdateExam(Exam exam);
 //查询所有试卷
 @Select("select * from xm_exam")
 List<Exam> AllExam();
 //根据试卷id查询
 @Select("select * from xm_exam where id=#{id}")
 Exam SelectExamById(Integer id);
 //根据课程id查询试卷
 @Select("select * from xm_exam where course_id=#{course_id}")
 List<Exam> SelectExamByCourseId(Integer course_id);
 //根据教师id查询试卷
 @Select("select * from xm_exam where teacher_id=#{teacher_id}")
 List<Exam> SelectExamByTeacherId(Integer teacher_id);
 //删除试卷
 @Delete("delete from xm_exam where id=#{id}")
 int DeleteExam(Integer id);
 //试卷的题目
 @Insert("insert into xm_exam_question (exam_id,question_id) values (#{exam_id},#{question_id})")
 int InsertExamQuestion(Integer exam_id,Integer question_id);
 //根据试卷id查询题目
 /**
  * 通过试卷ID获取试卷题目详细信息（完整信息）
  */
 @Select("SELECT " +
         "eq.id AS exam_question_id, " +
         "eq.score AS question_score, " +
         "eq.question_order, " +
         "qt.id AS question_id, " +
         "qt.questionType, " +
         "qt.text, " +
         "qt.optionA, " +
         "qt.optionB, " +
         "qt.optionC, " +
         "qt.optionD, " +
         "qt.optionE, " +
         "qt.optionF, " +
         "qt.optionG, " +
         "qt.answer, " +
         "qt.knowledge, " +
         "qt.difficulty, " +
         "e.title AS exam_title, " +
         "e.description AS exam_description, " +
         "e.total_score AS exam_total_score, " +
         "e.duration AS exam_duration, " +
         "e.pass_score AS exam_pass_score " +
         "FROM xm_exam_question eq " +
         "INNER JOIN xm_question_type qt ON eq.question_id = qt.id " +
         "INNER JOIN xm_exam e ON eq.exam_id = e.id " +
         "WHERE eq.exam_id = #{examId} " +
         "ORDER BY eq.question_order ASC")
 List<ExamQuestionVO> getExamQuestionsByExamId(@Param("examId") Integer examId);
    //根据试卷id删除题目
    @Delete("delete from xm_exam_question where exam_id=#{exam_id} and question_id=#{question_id}")
 Integer DeleteExamQuestion(Integer exam_id,Integer question_id);
 //------------------------有关请假------------------------------------------
 //添加请假记录
 @Insert("insert into vacation (userId,startTime,reason,type,status,time,endTime,day) values(#{userId},#{startTime},#{reason},#{type},#{status},#{time},#{endTime},#{day})")
 Integer addVacation(Vacation vacation);
    //获取请假记录
    @Select("select v.*,u.account,u.email from vacation v,user u where v.userId=u.id ")
    List<Vacation> getAllVacation();

    @Select("select * from vacation where userId=#{userId}")
    List<Vacation> getVacation(Integer userId);
    //审批
    @Update("update vacation set status=#{status} where id=#{id}")
    Integer updateVacation(Integer id,String status);
}
