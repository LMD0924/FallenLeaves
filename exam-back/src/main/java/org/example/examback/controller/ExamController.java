package org.example.examback.controller;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.example.examback.entity.Course;
import org.example.examback.entity.RestBean;
import org.example.examback.entity.User;
import org.example.examback.entity.ExamClass;
import org.example.examback.entity.Exam;
import org.example.examback.entity.ExamQuestionVO;
import org.example.examback.entity.Vacation;
import org.example.examback.entity.QuestionType;
import org.example.examback.service.ExamService;
import org.example.examback.service.UserService;
import org.example.examback.util.FileUploadUtil;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Arrays;
@Slf4j
@RestController
@RequestMapping("api/exam")
public class ExamController {
    @Resource
    ExamService examService;
    @Resource
    UserService userService;
    @Resource
    FileUploadUtil fileUploadUtil;
//----------------------有关课程-----------------------------------
     //添加课程
    @PostMapping("/InsertCourse")
    public RestBean<Integer> InsertCourse(Course course){
        int result=examService.InsertCourse(course);
        if(result!=0) return RestBean.success("添加课程成功",result);
        else return RestBean.failure(404,"添加课程失败");
    }
    //加入课程
    @PostMapping("InsertCourseUser")
    public RestBean<Integer> InsertCourseUser(@RequestParam("course_id") Integer course_id,
                                              @RequestParam("user_id") Integer user_id,
                                              @RequestParam("role") String role,
                                              @RequestParam("account") String account,
                                              @RequestParam(defaultValue = "审核未通过") String statue,
                                              HttpServletRequest request){
        Integer userId = (Integer) request.getAttribute("id");
        if(userId == null) {
            return RestBean.failure(401,"未登录或token无效");
        }
        User user=userService.getUserById(userId);
        if(user == null) {
            return RestBean.failure(404,"用户不存在");
        }
        if(user.getRole().equals("教师")) statue="审核通过";
        else statue="审核中";
        int result=examService.InsertCourseUser(course_id,user_id,role,account,statue);
        if(result!=0) return RestBean.success("加入课程成功",result);
        else return RestBean.failure(404,"加入课程失败");
    }
    //查询课程用户
    @GetMapping("SelectCourseUser")
    public RestBean<List<Course>> SelectCourseUser(@RequestParam("course_id") Integer course_id){
        System.out.println("课程id:"+course_id);
        List<Course> result=examService.SelectCourseUser(course_id);
        if(result!=null) return RestBean.success("查询成功",result);
        else return RestBean.failure(404,"查询失败");
    }
    //查询用户课程
    @GetMapping("SelectUserCourse")
    public RestBean<List<Course>> SelectUserCourse(HttpServletRequest request){
        Integer userId = (Integer) request.getAttribute("id");
        if(userId == null) {
            return RestBean.failure(401,"未登录或token无效");
        }
        List<Course> result=examService.SelectUserCourse(userId);
        if(result!=null) return RestBean.success("查询成功",result);
        else return RestBean.failure(404,"失败");
    }
    //退出课程
    @PostMapping("DeleteCourseUser")
    public RestBean<Integer> DeleteCourseUser(@RequestParam("user_id") Integer user_id,
                                          @RequestParam("course_id") Integer course_id){
        int result=examService.DeleteCourseUser(user_id,course_id);
        if(result!=0) return RestBean.success("退出成功",result);
        else return RestBean.failure(404,"退出失败");
    }
    //查询所有课程
    @GetMapping("/AllCourse")
    public RestBean<List<Course>> AllCourse(){
        List<Course> list=examService.AllCourse();
        return RestBean.success("查询成功",list);
    }
    //根据id查询课程
    @GetMapping("/SelectCourseById")
    public RestBean<Course> SelectCourseById(@RequestParam("id") Integer id){
        Course course=examService.SelectCourseById(id);
        if(course!=null) return RestBean.success("查询成功",course);
        else return RestBean.failure(404,"查询失败");
    }
    //根据教师id查询课程
    @GetMapping("/SelectCourseByTeacherId")
    public RestBean<List<Course>> SelectCourseByTeacherId(@RequestParam("teacher_id") Integer teacher_id){
        List<Course> list=examService.SelectCourseByTeacherId(teacher_id);
        return RestBean.success("查询成功",list);
    }
    //根据id修改课程
    @PostMapping("/UpdateCourse")
    public RestBean<Integer> UpdateCourse(Course course){
        int result=examService.UpdateCourse(course);
        if(result!=0) return RestBean.success("修改成功",result);
        else return RestBean.failure(404,"修改失败");
    }
//---------------------有关题目------------------------------
    //添加题目
    @PostMapping("/InsertQuestion")
    public RestBean<Integer> InsertQuestion(QuestionType questiontype){
        int result=examService.InsertQuestion(questiontype);
        if(result!=0) return RestBean.success("添加成功",result);
        else return RestBean.failure(404,"添加失败");
    }
    //查询所有题目
    @GetMapping("/AllQuestion")
    public RestBean<List<QuestionType>> AllQuestion(){
        List<QuestionType> list=examService.AllQuestion();
        if(list!=null) return RestBean.success("查询成功",list);
        else return RestBean.failure(404,"查询失败");
    }
    //根据id查询题目
    @GetMapping("/SelectQuestionById")
    public RestBean<QuestionType> SelectQuestionById(@RequestParam("id") Integer id){
        QuestionType result=examService.SelectQuestionById(id);
        if(result!=null) return RestBean.success("查询成功",result);
        else return RestBean.failure(404,"查询失败");
    }
    //根据课程id查询题目
    @GetMapping("/SelectQuestionByCourseId")
    public RestBean<List<QuestionType>> SelectQuestionByCourseId(@RequestParam("course_id") Integer course_id){
        List<QuestionType> list=examService.SelectQuestionByCourseId(course_id);
        if(list!=null) return RestBean.success("查询成功",list);
        else return RestBean.failure(404,"查询失败");
    }
    //根据教师id查询题目
    @GetMapping("/SelectQuestionByTeacherId")
    public RestBean<List<QuestionType>> SelectQuestionByTeacherId(@RequestParam("teacher_id") Integer teacher_id){
        List<QuestionType> list=examService.SelectQuestionByTeacherId(teacher_id);
        if(list!=null) return RestBean.success("查询成功",list);
        else return RestBean.failure(404,"查询失败");
    }
    //教师修改题目
    @PostMapping("/UpdateQuestion")
    public RestBean<Integer> UpdateQuestion(QuestionType questiontype){
        int result=examService.UpdateQuestion(questiontype);
        if(result!=0) return RestBean.success("修改成功",result);
        else return RestBean.failure(404,"修改失败");
    }
    //管理员修改状态
    @PostMapping("/UpdateQuestionStatus")
    public RestBean<Integer> UpdateQuestionStatus(@RequestParam("status") String status,
                                                  @RequestParam("id") Integer id){
        int result=examService.UpdateQuestionStatus(status,id);
        if(result!=0) return RestBean.success("修改成功",result);
        else return RestBean.failure(404,"修改失败");
    }
    //-------------------有关班级--------------------------------------
    //添加班级
    @PostMapping("/InsertClass")
    public RestBean<Integer> InsertClass(@RequestParam("name") String name,
                                         @RequestParam("teacher_id") Integer teacher_id,
                                         @RequestParam("introduction") String introduction,
                                         @RequestParam("status") String status,
                                         @RequestParam("major") String major,
                                         @RequestParam("grade") String grade){
        int result=examService.InsertClass(name,teacher_id,introduction,status,major,grade);
        if(result!=0) return RestBean.success("添加班级成功",result);
        else return RestBean.failure(404,"添加班级失败");
    }
    //修改班级
    @PostMapping("/UpdateClass")
    public RestBean<Integer> UpdateClass(ExamClass examclass){
        int result=examService.UpdateClass(examclass);
        if(result!=0) return RestBean.success("修改班级成功",result);
        else return RestBean.failure(404,"修改班级失败");
    }
    //删除班级
    @PostMapping("/DeleteClass")
    public RestBean<Integer> DeleteClass(@RequestParam("id") Integer id){
        int result=examService.DeleteClass(id);
        if(result!=0) return RestBean.success("删除班级成功",result);
        else return RestBean.failure(404,"删除班级失败");
    }
    //查询所有班级-
    @GetMapping("/AllClass")
    public RestBean<List<ExamClass>> AllClass(){
        List<ExamClass> list=examService.AllClass();
        if(list!=null) return RestBean.success("查询成功",list);
        else return RestBean.failure(404,"查询失败");
    }
    //用户加入班级（支持单个和批量）
    @PostMapping("/JoinClass")
    public RestBean<Integer> JoinClass(HttpServletRequest request){
        // 权限验证
        Integer userId = (Integer) request.getAttribute("id");
        if(userId == null) {
            return RestBean.failure(401,"未登录或token无效");
        }
        User user=userService.getUserById(userId);
        if(user == null) {
            return RestBean.failure(404,"用户不存在");
        }
        if(!user.getRole().equals("管理员") && !user.getRole().equals("教师")) {
            return RestBean.failure(403,"权限不足，只有管理员和教师可以操作");
        }
        
        Integer class_id = Integer.parseInt(request.getParameter("class_id"));
        
        // 获取id参数（支持数组格式和单个格式）
        Map<String, String[]> parameterMap = request.getParameterMap();
        List<String> idList = new ArrayList<>();
        
        // 处理数组格式的id参数（如id[0]=1&id[1]=2）
        for (String key : parameterMap.keySet()) {
            if (key.startsWith("id[")) {
                String[] values = parameterMap.get(key);
                if (values != null && values.length > 0) {
                    idList.add(values[0]);
                }
            }
        }
        
        // 处理单个id参数
        String idParam = request.getParameter("id");
        if (idParam != null) {
            if (idParam.contains(",")) {
                // 逗号分隔的字符串
                String[] ids = idParam.split(",");
                idList.addAll(Arrays.asList(ids));
            } else {
                // 单个id
                idList.add(idParam);
            }
        }
        
        int result;
        if (!idList.isEmpty()) {
            // 批量操作
            String[] ids = idList.toArray(new String[0]);
            result = examService.BatchJoinClass(class_id, ids);
        } else {
            return RestBean.failure(400,"参数格式错误");
        }
        
        if(result!=0) return RestBean.success("加入班级成功",result);
        else return RestBean.failure(404,"加入班级失败");
    }
    //退出班级（支持单个和批量）
    @PostMapping("/ExitClass")
    public RestBean<Integer> ExitClass(HttpServletRequest request){
        // 权限验证
        Integer userId = (Integer) request.getAttribute("id");
        if(userId == null) {
            return RestBean.failure(401,"未登录或token无效");
        }
        User user=userService.getUserById(userId);
        if(user == null) {
            return RestBean.failure(404,"用户不存在");
        }
        if(!user.getRole().equals("管理员") && !user.getRole().equals("教师")) {
            return RestBean.failure(403,"权限不足，只有管理员和教师可以操作");
        }
        
        Integer class_id = Integer.parseInt(request.getParameter("class_id"));
        
        // 获取id参数（支持数组格式和单个格式）
        Map<String, String[]> parameterMap = request.getParameterMap();
        List<String> idList = new ArrayList<>();
        
        // 处理数组格式的id参数（如id[0]=1&id[1]=2）
        for (String key : parameterMap.keySet()) {
            if (key.startsWith("id[")) {
                String[] values = parameterMap.get(key);
                if (values != null && values.length > 0) {
                    idList.add(values[0]);
                }
            }
        }
        
        // 处理单个id参数
        String idParam = request.getParameter("id");
        if (idParam != null) {
            if (idParam.contains(",")) {
                // 逗号分隔的字符串
                String[] ids = idParam.split(",");
                idList.addAll(Arrays.asList(ids));
            } else {
                // 单个id
                idList.add(idParam);
            }
        }
        
        int result;
        if (!idList.isEmpty()) {
            // 批量操作
            String[] ids = idList.toArray(new String[0]);
            result = examService.BatchExitClass(class_id, ids);
        } else {
            return RestBean.failure(400,"参数格式错误");
        }
        
        if(result!=0) return RestBean.success("退出班级成功",result);
        else return RestBean.failure(404,"退出班级失败");
    }
    //根据id查询班级
    @GetMapping("/SelectClassById")
    public RestBean<ExamClass> SelectClassById(Integer id){
        ExamClass examclass=examService.SelectClassById(id);
        if(examclass!=null) return RestBean.success("查询成功",examclass);
        else return RestBean.failure(404,"查询失败");
    }

    /*
    * 根据班级id查询学生
    * */
    @GetMapping("/SelectClassByUserId")
    public RestBean<List<User>> SelectClassByUserId(Integer id){
        List<User> list=examService.SelectClassByUserId(id);
        if(list!=null) return RestBean.success("查询成功",list);
        else return RestBean.failure(404,"查询失败");
    }
    //---------------------有关试卷--------------------------------------------------
    //添加试卷
    @PostMapping("/InsertExam")
    public RestBean<Integer> InsertExam(Exam exam){
        int result=examService.InsertExam(exam);
        if(result!=0) return RestBean.success("添加试卷成功",result);
        else return RestBean.failure(404,"添加试卷失败");
    }
    //修改试卷
    @PostMapping("/UpdateExam")
    public RestBean<Integer> UpdateExam(Exam exam){
        int result=examService.UpdateExam(exam);
        if(result!=0) return RestBean.success("修改试卷成功",result);
        else return RestBean.failure(404,"修改试卷失败");
    }
    //查询所有试卷
    @GetMapping("/AllExam")
    public RestBean<List<Exam>> AllExam(){
        List<Exam> list=examService.AllExam();
        if(list!=null) return RestBean.success("查询成功",list);
        else return RestBean.failure(404,"查询失败");
    }
    
    @GetMapping("/list")
    public RestBean<List<Exam>> listExam(){
        List<Exam> list=examService.AllExam();
        if(list!=null) return RestBean.success("查询成功",list);
        else return RestBean.failure(404,"查询失败");
    }
    //根据试卷id查询
    @GetMapping("/SelectExamById")
    public RestBean<Exam> SelectExamById(@RequestParam("id") Integer id){
        Exam exam=examService.SelectExamById(id);
        if(exam!=null) return RestBean.success("查询成功",exam);
        else return RestBean.failure(404,"查询失败");
    }
    //根据课程id查询试卷
    @GetMapping("/SelectExamByCourseId")
    public RestBean<List<Exam>> SelectExamByCourseId(@RequestParam("course_id") Integer course_id){
        List<Exam> list=examService.SelectExamByCourseId(course_id);
        if(list!=null) return RestBean.success("查询成功",list);
        else return RestBean.failure(404,"查询失败");
    }
    //根据教师id查询试卷
    @GetMapping("/SelectExamByTeacherId")
    public RestBean<List<Exam>> SelectExamByTeacherId(@RequestParam("teacher_id") Integer teacher_id){
        List<Exam> list=examService.SelectExamByTeacherId(teacher_id);
        if(list!=null) return RestBean.success("查询成功",list);
        else return RestBean.failure(404,"查询失败");
    }
    //删除试卷
    @PostMapping("/DeleteExam")
    public RestBean<Integer> DeleteExam(@RequestParam("id") Integer id){
        int result=examService.DeleteExam(id);
        if(result!=0) return RestBean.success("删除试卷成功",result);
        else return RestBean.failure(404,"删除试卷失败");
    }
    //添加试卷题目
    @PostMapping("/InsertExamQuestion")
    public RestBean<Integer> InsertExamQuestion(@RequestParam("exam_id") Integer exam_id,
                                                @RequestParam("question_id") Integer question_id){
        int result=examService.InsertExamQuestion(exam_id,question_id);
        if(result!=0) return RestBean.success("添加试卷题目成功",result);
        else return RestBean.failure(404,"添加试卷题目失败");
    }
    //根据试卷id查询题目
    @GetMapping("/getExamQuestionsByExamId")
    public RestBean<List<ExamQuestionVO>> getExamQuestionsByExamId(@RequestParam("exam_id") Integer exam_id){
        List<ExamQuestionVO> list=examService.getExamQuestionsByExamId(exam_id);
        if(list!=null) return RestBean.success("查询成功",list);
        else return RestBean.failure(404,"查询失败");
    }
    //根据试卷id删除题目
    @PostMapping("/DeleteExamQuestion")
    public RestBean<Integer> DeleteExamQuestion(@RequestParam("exam_id") Integer exam_id,
                                                @RequestParam("question_id") Integer question_id){
        int result=examService.DeleteExamQuestion(exam_id,question_id);
        if(result!=0) return RestBean.success("删除试卷题目成功",result);
        else return RestBean.failure(404,"删除试卷题目失败");
    }
    //----------------------------有关请假---------------------------------------------------------
    //添加请假记录
    @PostMapping("/InsertVacation")
    public RestBean<Integer> InsertVacation(Vacation vacation){
        int result=examService.addVacation(vacation);
        if(result!=0) return RestBean.success("添加请假记录成功",result);
        else return RestBean.failure(404,"添加请假记录失败");
    }
    //获取请假记录
    @GetMapping("/GetVacation")
    public RestBean<List<Vacation>> GetVacation(HttpServletRequest request){
        Integer userId = (Integer) request.getAttribute("id");
        if(userId == null) {
            return RestBean.failure(401,"未登录或token无效");
        }
        User user=userService.getUserById(userId);
        if(user == null) {
            return RestBean.failure(404,"用户不存在");
        }
        List<Vacation> list;
        if(user.getRole().equals("管理员")){
            list = examService.getAllVacation();
        }else{
            list = examService.getVacation(userId);
        }
        if(list!=null) return RestBean.success("查询成功",list);
        else return RestBean.failure(404,"查询失败");
    }
    //审批
    @PostMapping("/UpdateVacation")
    public RestBean<Integer> UpdateVacation(@RequestParam("id") Integer id,
                                         @RequestParam("status") String status,
                                         HttpServletRequest request){
        Integer userId = (Integer) request.getAttribute("id");
        if(userId == null) {
            return RestBean.failure(401,"未登录或token无效");
        }
        User user=userService.getUserById(userId);
        if(user == null) {
            return RestBean.failure(404,"用户不存在");
        }
        if(!(user.getRole().equals("管理员"))&&status.equals("已注销")){
            int result=examService.updateVacation(id,status);
            if(result!=0) return RestBean.success("注销成功",result);
            else return RestBean.failure(404,"注销失败");
        }else if(user.getRole().equals("管理员")){
            int result=examService.updateVacation(id,status);
            if(result!=0) return RestBean.success("审核成功",result);
            else return RestBean.failure(404,"审核失败");
        }else return RestBean.failure(404,"权限不足");
    }
    //----------------------------有关异常处理---------------------------------------------------------
        @ExceptionHandler(Exception.class)
    public RestBean<String> exceptionHandler(Exception ex){
        log.error("系统异常：", ex);  // 记录完整异常堆栈
        return RestBean.failure("系统错误：" + (ex.getMessage() != null ? ex.getMessage() : "未知错误"));
        }
}
