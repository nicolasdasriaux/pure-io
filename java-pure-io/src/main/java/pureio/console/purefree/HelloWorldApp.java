package pureio.console.purefree;

import static pureio.console.purefree.Console.getStrLn;
import static pureio.console.purefree.Console.putStrLn;

@SuppressWarnings({"CodeBlock2Expr", "WeakerAccess", "Convert2MethodRef", "Duplicates"})
public class HelloWorldApp {
    public static final Program<Unit> helloApp =
            putStrLn("What's your name?").thenChain(__ -> {
                return getStrLn().thenChain(name -> {
                    return putStrLn("Hello " + name + "!");
                });
            });

    public static void main(String[] args) {
        final Program<Unit> program = helloApp;
        // PURE, anything above done by creating and combining programs
        Program.unsafeRun(program); // IMPURE, only at the Edge of the World
    }
}
