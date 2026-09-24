package datastructure.user;
import model.User; import java.util.*;
public class UserSearch {
    private static final int TABLE_SIZE=101; private final UserEntry[] table=new UserEntry[TABLE_SIZE]; private int size;
    private static class UserEntry{User user;UserEntry next;UserEntry(User u){user=u;}}
    private int hash(String email){if(email==null)return 0;int h=0;for(int i=0;i<email.length();i++)h=31*h+Character.toLowerCase(email.charAt(i));return Math.abs(h)%TABLE_SIZE;}
    public synchronized void addUser(User user){if(user==null||user.getEmail()==null)return;String e=user.getEmail().trim().toLowerCase();int i=hash(e);UserEntry c=table[i];while(c!=null){if(c.user.getEmail().equalsIgnoreCase(e)){c.user=user;return;}c=c.next;}UserEntry n=new UserEntry(user);n.next=table[i];table[i]=n;size++;}
    public synchronized User searchUser(String email){if(email==null||email.trim().isEmpty())return null;String e=email.trim().toLowerCase();UserEntry c=table[hash(e)];while(c!=null){if(c.user.getEmail().equalsIgnoreCase(e))return c.user;c=c.next;}return null;}
    public synchronized User login(String email, String password) {
        User u = searchUser(email);
        if (u != null && password != null && security.PasswordUtil.verifyPassword(password, u.getPassword())) {
            if (u.getPassword() != null && !u.getPassword().startsWith("$pbkdf2$")) {
                String hashed = security.PasswordUtil.hashPassword(password);
                u.setPassword(hashed);
            }
            return u;
        }
        return null;
    }
    public void clearIfSupported(){java.util.Arrays.fill(table,null);size=0;}
    public int getSize(){return size;}
    public int getUserCount(){return size;}
}
