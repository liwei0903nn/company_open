package com.cn.hnust.controller;


import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.cn.hnust.pojo.Img;
import com.cn.hnust.pojo.News;
import com.cn.hnust.pojo.User;
import com.cn.hnust.service.IImgService;
import com.cn.hnust.service.INewsService;
import com.cn.hnust.service.IUserService;
import com.cn.hnust.util.AbstractController;
import com.sun.org.apache.regexp.internal.recompile;
import com.sun.org.apache.xml.internal.serializer.ElemDesc;

@Controller
@RequestMapping("/index")
public class IndexController extends AbstractController {
	
	@Resource
	private IImgService imgService ;
	
	@Resource
	private INewsService newService ;
	
	@Resource
	private IUserService userService;
	
	@RequestMapping("/turnToIndex/{pageNum}")
	public String trunToIndex(@PathVariable int pageNum,Model model,HttpSession session){
		Img img = new Img() ;
//		设置为1表示自查询上首页的图片
		img.setIs_index("1");
		List<Img> imgs = imgService.findByIndex(img) ;
		model.addAttribute("imgs", imgs) ;
		//查询最新的新闻信息。
		News ns = new News() ;
		super.setPageNum(pageNum) ;
		super.setRowCount(newService.findAllCount()) ;
		super.getIndex() ;
		ns.setStartIndex(super.getStartIndex()) ;
		ns.setEndIndex(super.getEndIndex()) ;
		List<News> news = newService.findAll(ns) ;
		model.addAttribute("news", news) ;
		model.addAttribute("currentpage", pageNum) ;
		User user = (User) session.getAttribute("user") ;
		if(user!= null){
			int userid = user.getId() ;
			User userById = userService.getUserById(userid) ;
			String img_id = userById.getImg_id() ;
			if(img_id != null){
				String path = imgService.selectByPrimaryKey(Integer.parseInt(img_id)).getPath() ;
				model.addAttribute("headimg", path) ;
			}
			
		}
		
		return "../../index" ;
	}
	
	/**
	 * 
	 * @Description: 检查用户名或者是邮箱是否重复
	 * @param @param u
	 * @param @param response
	 * @param @throws IOException   
	 * @return void  
	 * @throws
	 * @author chj
	 * @date 2016-1-24  下午1:08:17
	 */
	@RequestMapping("/checkIsRepeat")
	public void checkIsRepeat(User u,HttpServletResponse response) throws IOException{
		response.setCharacterEncoding("utf-8") ;
		response.setContentType("html/text") ;
		int count = userService.findAllCount(u) ;
		if(count ==0){
			response.getWriter().print("true") ;
		}else {
			response.getWriter().print("false") ;
		}
	}
	
	@RequestMapping("/register")
	public String register(User u,HttpServletRequest request) throws IOException{
		userService.createUser(u) ;
//		注册完之后直接登录
		request.getSession().setAttribute("user", u) ;
//		重定向到首页
		return "redirect:../index/turnToIndex/1" ;
	}
	
	@RequestMapping("/login")
	public void login(User u,HttpServletRequest request,HttpServletResponse response) throws IOException{
		User user = userService.findByLogin(u) ;
		response.setContentType("text/html");
	    response.setCharacterEncoding("utf-8");
		if(user != null){
			request.getSession().setAttribute("user", user) ;
			response.getWriter().print("true") ;
//			return "redirect:../index/turnToIndex/1" ;
		}else {
		    response.getWriter().print("false") ;
		}
	}
	
	/**
	 * 
	 * @Description: 修改密码
	 * @param @param user
	 * @param @param session
	 * @param @return   
	 * @return String  
	 * @throws
	 * @author chj
	 * @date 2016-1-26  下午7:41:56
	 */
	@RequestMapping("/updateUserInfo")
	public String updateUserInfo(User user,String flag ,HttpSession session){
		userService.updateByPrimaryKeySelective(user) ;
		if(flag.equals("0")){//为0就是修改密码
			session.removeAttribute("user") ;
			return "redirect:../index/turnToIndex/1" ;
			
		}else{//为1的话表示更新基本设置 刷新界面就可以了 不需要去掉session
			return null ;
		}
	}
	
	/**
	 * 
	 * @Description: 修改基本设置 包括头像
	 * @param @param file
	 * @param @param user
	 * @param @return   
	 * @return String  
	 * @throws
	 * @author chj
	 * @date 2016-1-27  下午2:10:48
	 */
	@RequestMapping("/updateBaseUserInfo")
	public String updateBaseUserInfo(User user,HttpServletRequest request){
		
		userService.updateByPrimaryKeySelective(user) ;
		return "redirect:../user/frontUserSet/"+user.getId() ;
	}
	
	@RequestMapping("/updateHeadImg")
	public String updateHeadImg(@RequestParam(value="file")MultipartFile file,HttpServletRequest request,Img img,User user){
		String path = request.getSession().getServletContext().getRealPath("upload/headimg"); 
		String fileName = file.getOriginalFilename() ;
		File targetFile = new File(path, fileName); 
		if(!targetFile.exists()){  
            targetFile.mkdirs();  
        }
		try {
			file.transferTo(targetFile) ;
		} catch (Exception e) {
			e.printStackTrace() ;
		}
		img.setPath("upload/headimg/"+fileName) ;
		DecimalFormat df = new DecimalFormat("######0.00");   
		double size = Double.parseDouble(file.getSize()+"") ;
		double dbsize = size /(1024*1024) ;
		String strsize = df.format(dbsize) ;
		img.setImg_size(strsize+"MB") ;
		img.setCreate_date(new Date()) ;
		img.setIs_index("0") ;//默认为 不是首页置顶
		img.setName(user.getId()+"的头像") ;
		imgService.insertSelective(img) ;
		int img_id = img.getId() ;
		if(img_id != 0){
			user.setImg_id(img_id+"") ;
		}
		userService.updateByPrimaryKeySelective(user) ;
		return "redirect:../user/frontUserSet/"+user.getId() ;
	}
	
	/**
	 * 
	 * @Description: 验证当前密码是否错误
	 * @param @param user
	 * @param @param response
	 * @param @throws IOException   
	 * @return void  
	 * @throws
	 * @author chj
	 * @date 2016-1-26  下午7:47:52
	 */
	@RequestMapping("/checkCurrentPwd")
	public void checkCurrentPwd(User user,HttpServletResponse response) throws IOException{
		response.setContentType("text/html");
	    response.setCharacterEncoding("utf-8");
	    int count = userService.findAllCount(user) ;
	    if(count ==0){
	    	response.getWriter().print("false") ;
	    }else {
	    	response.getWriter().print("true") ;
	    }
	}
	
	
}
