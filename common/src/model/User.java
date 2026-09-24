package model;

public class User {
    private int id; private String userCode; private String fullName; private String email; private String password;
    private String department; private String division; private String designation; private String role;
    public User(int id,String userCode,String fullName,String email,String password,String department,String division,String designation,String role){
        this.id=id;this.userCode=userCode;this.fullName=fullName;this.email=email;this.password=password;this.department=department;this.division=division;this.designation=designation;this.role=role;
    }
    public int getId(){return id;} public String getUserCode(){return userCode;} public String getFullName(){return fullName;} public String getEmail(){return email;}
    public String getPassword(){return password;} public void setPassword(String password){this.password=password;} public String getDepartment(){return department;} public String getDivision(){return division;}
    public String getDesignation(){return designation;} public String getRole(){return role;}
}
