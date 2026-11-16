import org.objectweb.asm.*;
import java.io.*; import java.nio.file.*; import java.util.*; import java.util.zip.*;
/** usage: ATApply <in.jar> <out.jar> <at.cfg (moj names)> ; widens access per AT lines (class / field / method name-only match). */
public class ATApply {
  record Rule(String acc, boolean definal){}
  static Map<String,Rule> cls=new HashMap<>(), mem=new HashMap<>();
  public static void main(String[] a) throws Exception {
    for (String l: Files.readAllLines(Path.of(a[2]))) {
      l=l.replaceAll("#.*","").trim(); if(l.isEmpty()) continue;
      String[] p=l.split("\\s+"); String acc=p[0]; boolean df=acc.endsWith("-f"); acc=acc.replace("-f","").replace("+f","");
      String c=p[1].replace('.','/');
      if(p.length==2) cls.put(c,new Rule(acc,df));
      else { String n=p[2]; int i=n.indexOf('('); if(i>0) n=n.substring(0,i); mem.put(c+"."+n,new Rule(acc,df)); cls.putIfAbsent(c, null); }
    }
    try(ZipInputStream in=new ZipInputStream(new FileInputStream(a[0])); ZipOutputStream out=new ZipOutputStream(new FileOutputStream(a[1]))){
      ZipEntry e; while((e=in.getNextEntry())!=null){
        byte[] b=in.readAllBytes(); String n=e.getName();
        if(n.endsWith(".class")){ String cn=n.substring(0,n.length()-6); String outer=cn.contains("$")?cn:cn;
          if(cls.containsKey(cn) || hasMembers(cn) || hasInner(cn)) b=patch(b,cn); }
        out.putNextEntry(new ZipEntry(n)); out.write(b); out.closeEntry(); }
    }
  }
  static boolean hasInner(String c){ for(String k:cls.keySet()) if(k.startsWith(c+"$")) return true; return false; }
  static boolean hasMembers(String c){ for(String k:mem.keySet()) if(k.startsWith(c+".")) return true; return false; }
  static int widen(int access, Rule r){
    if(r==null) return access;
    int a=access & ~(Opcodes.ACC_PRIVATE|Opcodes.ACC_PROTECTED|Opcodes.ACC_PUBLIC);
    switch(r.acc){ case "public": a|=Opcodes.ACC_PUBLIC; break; case "protected": a|= (access&Opcodes.ACC_PUBLIC)!=0?Opcodes.ACC_PUBLIC:Opcodes.ACC_PROTECTED; break; default: a = access & ~Opcodes.ACC_PRIVATE; }
    if(r.definal) a&=~Opcodes.ACC_FINAL; return a; }
  static byte[] patch(byte[] b, String cn){
    ClassReader cr=new ClassReader(b); ClassWriter cw=new ClassWriter(0);
    cr.accept(new ClassVisitor(Opcodes.ASM9,cw){
      public void visit(int v,int acc,String name,String sig,String sup,String[] itf){ super.visit(v, widen(acc,cls.get(cn)), name,sig,sup,itf); }
      public void visitInnerClass(String name,String outer,String inner,int acc){ super.visitInnerClass(name,outer,inner, widen(acc,cls.get(name))); }
      public FieldVisitor visitField(int acc,String name,String d,String s,Object v){ return super.visitField(widen(acc,mem.get(cn+"."+name)),name,d,s,v); }
      public MethodVisitor visitMethod(int acc,String name,String d,String s,String[] ex){ return super.visitMethod(widen(acc,mem.get(cn+"."+name)),name,d,s,ex); }
    },0); return cw.toByteArray(); }
}
