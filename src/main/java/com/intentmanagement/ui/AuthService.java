package com.intentmanagement.ui;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.util.*;

/** Local prototype account store. Passwords are salted PBKDF2 hashes, never plaintext. */
public final class AuthService {
    private final Path file=Path.of("data","auth","users.properties");
    private final Properties users=new Properties();
    private final SecureRandom random=new SecureRandom();
    public AuthService(){try{Files.createDirectories(file.getParent());if(Files.exists(file))try(var in=Files.newInputStream(file)){users.load(in);}}catch(Exception e){throw new IllegalStateException("Cannot load local account store",e);}}
    public synchronized boolean signup(String name,String email,char[] password){String key=norm(email);if(name==null || name.isBlank() || !key.matches("^[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,}$")
                || password==null || password.length<8 || users.containsKey(key)) return false;byte[] salt=new byte[16];random.nextBytes(salt);users.setProperty(key,name.trim()+"|"+Base64.getEncoder().encodeToString(salt)+"|"+hash(password,salt));save();return true;}
    public synchronized boolean login(String email,char[] password){
        String record=users.getProperty(norm(email));
        if(record==null || password==null) return false;try{String[] p=record.split("\\|",-1);return MessageDigest.isEqual(Base64.getDecoder().decode(p[2]),Base64.getDecoder().decode(hash(password,Base64.getDecoder().decode(p[1]))));}catch(Exception e){return false;}}
    public synchronized boolean exists(String email){return users.containsKey(norm(email));}
    public synchronized boolean reset(String email,char[] password){String key=norm(email),record=users.getProperty(key);if(record==null||password==null||password.length<8)return false;String name=record.split("\\|",-1)[0];byte[] salt=new byte[16];random.nextBytes(salt);users.setProperty(key,name+"|"+Base64.getEncoder().encodeToString(salt)+"|"+hash(password,salt));save();return true;}
    private String hash(char[] password,byte[] salt){try{PBEKeySpec spec=new PBEKeySpec(password,salt,120000,256);byte[] out=SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();spec.clearPassword();return Base64.getEncoder().encodeToString(out);}catch(Exception e){throw new IllegalStateException(e);}}
    private String norm(String s){return s==null?"":s.trim().toLowerCase(Locale.ROOT);}
    private void save(){try{Path tmp=file.resolveSibling("users.properties.tmp");try(var out=Files.newOutputStream(tmp)){users.store(out,"IntentOps local accounts; password values are salted PBKDF2 hashes");}try{Files.move(tmp,file,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}catch(AtomicMoveNotSupportedException e){Files.move(tmp,file,StandardCopyOption.REPLACE_EXISTING);}}catch(Exception e){throw new IllegalStateException("Cannot save local account store",e);}}
}
