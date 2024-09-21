package com.hms.main.student.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.hms.common.annotation.Excel;
import com.hms.common.core.domain.BaseEntity;

/**
 * 学生信息对象 hms_student
 * 
 * @author CYQ
 * @date 2024-09-13
 */
public class HmsStudent extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /**  */
    private Long id;

    /** 学号 */
    @Excel(name = "学号")
    private String studentNumber;

    /** 姓名 */
    @Excel(name = "姓名")
    private String name;

    /** 性别 */
    @Excel(name = "性别")
    private Integer gender;

    /** 出生日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "出生日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date birthdate;

    /** 入学日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "入学日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date admissionDate;

    /** 年级 */
    @Excel(name = "年级")
    private String grade;

    /** 班级 */
    @Excel(name = "班级")
    private String classname;

    /** 专业 */
    @Excel(name = "专业")
    private String major;

    /** 学生状态 */
    @Excel(name = "学生状态")
    private Integer status;

    /** 电子邮件 */
    @Excel(name = "电子邮件")
    private String email;

    /** 电话号码 */
    @Excel(name = "电话号码")
    private String phone;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "创建时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date createdAt;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "更新时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date updatedAt;

    /** 自定义字段1 */
    private String def1;

    /** 自定义字段2 */
    private String def2;

    /** 自定义字段3 */
    private String def3;

    /** 自定义字段4 */
    private String def4;

    /** 自定义字段5 */
    private String def5;

    public void setId(Long id) 
    {
        this.id = id;
    }

    public Long getId() 
    {
        return id;
    }
    public void setStudentNumber(String studentNumber) 
    {
        this.studentNumber = studentNumber;
    }

    public String getStudentNumber() 
    {
        return studentNumber;
    }
    public void setName(String name) 
    {
        this.name = name;
    }

    public String getName() 
    {
        return name;
    }
    public void setGender(Integer gender) 
    {
        this.gender = gender;
    }

    public Integer getGender() 
    {
        return gender;
    }
    public void setBirthdate(Date birthdate) 
    {
        this.birthdate = birthdate;
    }

    public Date getBirthdate() 
    {
        return birthdate;
    }
    public void setAdmissionDate(Date admissionDate) 
    {
        this.admissionDate = admissionDate;
    }

    public Date getAdmissionDate() 
    {
        return admissionDate;
    }
    public void setGrade(String grade) 
    {
        this.grade = grade;
    }

    public String getGrade() 
    {
        return grade;
    }
    public void setClassname(String classname) 
    {
        this.classname = classname;
    }

    public String getClassname() 
    {
        return classname;
    }
    public void setMajor(String major) 
    {
        this.major = major;
    }

    public String getMajor() 
    {
        return major;
    }
    public void setStatus(Integer status) 
    {
        this.status = status;
    }

    public Integer getStatus() 
    {
        return status;
    }
    public void setEmail(String email) 
    {
        this.email = email;
    }

    public String getEmail() 
    {
        return email;
    }
    public void setPhone(String phone) 
    {
        this.phone = phone;
    }

    public String getPhone() 
    {
        return phone;
    }
    public void setCreatedAt(Date createdAt) 
    {
        this.createdAt = createdAt;
    }

    public Date getCreatedAt() 
    {
        return createdAt;
    }
    public void setUpdatedAt(Date updatedAt) 
    {
        this.updatedAt = updatedAt;
    }

    public Date getUpdatedAt() 
    {
        return updatedAt;
    }
    public void setDef1(String def1) 
    {
        this.def1 = def1;
    }

    public String getDef1() 
    {
        return def1;
    }
    public void setDef2(String def2) 
    {
        this.def2 = def2;
    }

    public String getDef2() 
    {
        return def2;
    }
    public void setDef3(String def3) 
    {
        this.def3 = def3;
    }

    public String getDef3() 
    {
        return def3;
    }
    public void setDef4(String def4) 
    {
        this.def4 = def4;
    }

    public String getDef4() 
    {
        return def4;
    }
    public void setDef5(String def5) 
    {
        this.def5 = def5;
    }

    public String getDef5() 
    {
        return def5;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("studentNumber", getStudentNumber())
            .append("name", getName())
            .append("gender", getGender())
            .append("birthdate", getBirthdate())
            .append("admissionDate", getAdmissionDate())
            .append("grade", getGrade())
            .append("classname", getClassname())
            .append("major", getMajor())
            .append("status", getStatus())
            .append("email", getEmail())
            .append("phone", getPhone())
            .append("createdAt", getCreatedAt())
            .append("updatedAt", getUpdatedAt())
            .append("def1", getDef1())
            .append("def2", getDef2())
            .append("def3", getDef3())
            .append("def4", getDef4())
            .append("def5", getDef5())
            .toString();
    }
}
