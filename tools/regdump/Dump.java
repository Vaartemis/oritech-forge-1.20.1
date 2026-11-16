import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.SharedConstants;
public class Dump { public static void main(String[] a) throws Exception {
 SharedConstants.tryDetectVersion(); net.minecraft.server.Bootstrap.bootStrap();
 try (var w = new java.io.PrintWriter("ids.txt")) {
  for (var r : BuiltInRegistries.REGISTRY)
   for (var k : r.keySet()) w.println(r.key().location().getPath()+" "+k);
 }}}
