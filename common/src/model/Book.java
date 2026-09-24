package model;

public class Book {
    private int id;
    private String bookId;
    private String title;
    private String author;
    private String isbn;
    private String category;
    private String department;
    private String rackNumber;
    private int totalQuantity;
    private int availableQuantity;

    public Book(int id, String bookId, String title, String author, String isbn,
                String category, int totalQuantity, int availableQuantity) {
        this.id=id; this.bookId=bookId; this.title=title; this.author=author; this.isbn=isbn;
        this.category=category; this.totalQuantity=totalQuantity; this.availableQuantity=availableQuantity;
        this.department = "Computer Engineering";
        this.rackNumber = "R-01";
    }
    public Book(String bookId,String title,String author,String isbn,String category,int totalQuantity,int availableQuantity){
        this(0,bookId,title,author,isbn,category,totalQuantity,availableQuantity);
    }
    public int getId(){return id;} public String getBookId(){return bookId;} public String getTitle(){return title;}
    public String getAuthor(){return author;} public String getIsbn(){return isbn;} public String getCategory(){return category;}
    public String getDepartment(){return department;} public String getRackNumber(){return rackNumber;}
    public int getTotalQuantity(){return totalQuantity;} public int getAvailableQuantity(){return availableQuantity;}
    public void setId(int v){id=v;} public void setDepartment(String v){department=v;} public void setRackNumber(String v){rackNumber=v;}
    public void setAvailableQuantity(int v){availableQuantity=v;} public void setTotalQuantity(int v){totalQuantity=v;}
}
