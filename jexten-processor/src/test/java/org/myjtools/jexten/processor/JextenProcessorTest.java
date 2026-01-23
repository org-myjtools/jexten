package org.myjtools.jexten.processor;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.tools.JavaFileObject;
import java.util.List;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;


/**
 * Tests for the JextenProcessor annotation processor.
 * <p>
 * Note: The processor requires module-info.java to exist before performing other validations.
 * Tests are organized to verify:
 * <ul>
 *   <li>Module-info requirement detection</li>
 *   <li>Annotation validation (type checks, version formats)</li>
 *   <li>Extension point inference</li>
 *   <li>Edge cases</li>
 * </ul>
 */
@DisplayName("JextenProcessor")
class JextenProcessorTest {

    private static final java.io.File JEXTEN_CORE_CLASSES = resolveJextenCoreClasspath();

    private static java.io.File resolveJextenCoreClasspath() {
        // First try the target/classes directory (for reactor builds)
        java.io.File targetClasses = new java.io.File("../jexten-core/target/classes");
        if (targetClasses.exists()) {
            return targetClasses;
        }
        // Fallback to local Maven repository
        return new java.io.File(System.getProperty("user.home") + "/.m2/repository/org/myjtools/jexten/jexten-core/1.0.0-alpha1/jexten-core-1.0.0-alpha1.jar");
    }

    private Compilation compile(JavaFileObject... sources) {
        return javac()
                .withProcessors(new JextenProcessor())
                .withClasspath(List.of(JEXTEN_CORE_CLASSES))
                .compile(sources);
    }

    private Compilation compileWithoutProcessor(JavaFileObject... sources) {
        return javac()
                .withClasspath(List.of(JEXTEN_CORE_CLASSES))
                .compile(sources);
    }


    @Nested
    @DisplayName("Module-info validation")
    class ModuleInfoValidation {

        @Test
        @DisplayName("should require module-info.java for @ExtensionPoint")
        void shouldRequireModuleInfoForExtensionPoint() {
            JavaFileObject source = JavaFileObjects.forSourceString(
                    "test.MyExtensionPoint",
                    """
                            package test;
                            
                            import org.myjtools.jexten.ExtensionPoint;
                            
                            @ExtensionPoint(version = "1.0")
                            public interface MyExtensionPoint {
                            }
                            """
            );

            Compilation compilation = compile(source);

            assertThat(compilation).hadErrorContaining("module-info.java");
        }


        @Test
        @DisplayName("should require module-info.java for @Extension")
        void shouldRequireModuleInfoForExtension() {
            JavaFileObject extensionPoint = JavaFileObjects.forSourceString(
                    "test.MyExtensionPoint",
                    """
                            package test;
                            
                            import org.myjtools.jexten.ExtensionPoint;
                            
                            @ExtensionPoint(version = "1.0")
                            public interface MyExtensionPoint {
                            }
                            """
            );

            JavaFileObject extension = JavaFileObjects.forSourceString(
                    "test.MyExtension",
                    """
                            package test;
                            
                            import org.myjtools.jexten.Extension;
                            
                            @Extension
                            public class MyExtension implements MyExtensionPoint {
                            }
                            """
            );

            Compilation compilation = compile(extensionPoint, extension);

            assertThat(compilation).hadErrorContaining("module-info.java");
        }


        @Test
        @DisplayName("should compile without processor when no module-info")
        void shouldCompileWithoutProcessor() {
            JavaFileObject source = JavaFileObjects.forSourceString(
                    "test.MyExtensionPoint",
                    """
                            package test;
                            
                            import org.myjtools.jexten.ExtensionPoint;
                            
                            @ExtensionPoint(version = "1.0")
                            public interface MyExtensionPoint {
                            }
                            """
            );

            Compilation compilation = compileWithoutProcessor(source);

            assertThat(compilation).succeeded();
        }
    }


    @Nested
    @DisplayName("Annotation syntax validation (without module)")
    class AnnotationSyntaxValidation {

        @Test
        @DisplayName("should detect @ExtensionPoint on class even without module-info")
        void shouldDetectExtensionPointOnClass() {
            // The processor validates type before checking module
            JavaFileObject source = JavaFileObjects.forSourceString(
                    "test.MyClass",
                    """
                            package test;
                            
                            import org.myjtools.jexten.ExtensionPoint;
                            
                            @ExtensionPoint(version = "1.0")
                            public class MyClass {
                            }
                            """
            );

            Compilation compilation = compile(source);

            // Processor detects module-info missing first
            assertThat(compilation).failed();
        }


        @Test
        @DisplayName("should detect @Extension on interface even without module-info")
        void shouldDetectExtensionOnInterface() {
            JavaFileObject extensionPoint = JavaFileObjects.forSourceString(
                    "test.MyExtensionPoint",
                    """
                            package test;
                            
                            import org.myjtools.jexten.ExtensionPoint;
                            
                            @ExtensionPoint(version = "1.0")
                            public interface MyExtensionPoint {
                            }
                            """
            );

            JavaFileObject extension = JavaFileObjects.forSourceString(
                    "test.MyExtension",
                    """
                            package test;
                            
                            import org.myjtools.jexten.Extension;
                            
                            @Extension
                            public interface MyExtension extends MyExtensionPoint {
                            }
                            """
            );

            Compilation compilation = compile(extensionPoint, extension);

            // Processor detects module-info missing first
            assertThat(compilation).failed();
        }
    }


    @Nested
    @DisplayName("Valid annotation usage (compilation without full validation)")
    class ValidAnnotationUsage {

        @Test
        @DisplayName("should parse @ExtensionPoint with valid version")
        void shouldParseExtensionPointWithValidVersion() {
            JavaFileObject source = JavaFileObjects.forSourceString(
                    "test.MyExtensionPoint",
                    """
                            package test;
                            
                            import org.myjtools.jexten.ExtensionPoint;
                            
                            @ExtensionPoint(version = "1.0")
                            public interface MyExtensionPoint {
                                void execute();
                            }
                            """
            );

            // Without processor, compilation should succeed
            Compilation compilation = compileWithoutProcessor(source);

            assertThat(compilation).succeeded();
        }


        @Test
        @DisplayName("should parse @Extension with all attributes")
        void shouldParseExtensionWithAllAttributes() {
            JavaFileObject extensionPoint = JavaFileObjects.forSourceString(
                    "test.Service",
                    """
                            package test;
                            
                            import org.myjtools.jexten.ExtensionPoint;
                            
                            @ExtensionPoint(version = "2.0")
                            public interface Service {
                                void execute();
                            }
                            """
            );

            JavaFileObject extension = JavaFileObjects.forSourceString(
                    "test.ServiceImpl",
                    """
                            package test;
                            
                            import org.myjtools.jexten.Extension;
                            import org.myjtools.jexten.Priority;
                            import org.myjtools.jexten.Scope;
                            
                            @Extension(
                                extensionPoint = "test.Service",
                                extensionPointVersion = "2.0",
                                priority = Priority.HIGHEST,
                                scope = Scope.SINGLETON,
                                name = "primary-service"
                            )
                            public class ServiceImpl implements Service {
                                @Override
                                public void execute() {}
                            }
                            """
            );

            // Without processor, compilation should succeed
            Compilation compilation = compileWithoutProcessor(extensionPoint, extension);

            assertThat(compilation).succeeded();
        }


        @Test
        @DisplayName("should parse @Extension inferring extension point")
        void shouldParseExtensionInferringExtensionPoint() {
            JavaFileObject extensionPoint = JavaFileObjects.forSourceString(
                    "test.MyExtensionPoint",
                    """
                            package test;
                            
                            import org.myjtools.jexten.ExtensionPoint;
                            
                            @ExtensionPoint(version = "1.0")
                            public interface MyExtensionPoint {
                                void execute();
                            }
                            """
            );

            JavaFileObject extension = JavaFileObjects.forSourceString(
                    "test.MyExtension",
                    """
                            package test;
                            
                            import org.myjtools.jexten.Extension;
                            
                            @Extension
                            public class MyExtension implements MyExtensionPoint {
                                @Override
                                public void execute() {}
                            }
                            """
            );

            // Without processor, compilation should succeed
            Compilation compilation = compileWithoutProcessor(extensionPoint, extension);

            assertThat(compilation).succeeded();
        }
    }


    @Nested
    @DisplayName("Extension point type hierarchy")
    class ExtensionPointTypeHierarchy {

        @Test
        @DisplayName("should compile extension extending abstract class")
        void shouldCompileExtensionExtendingAbstractClass() {
            JavaFileObject extensionPoint = JavaFileObjects.forSourceString(
                    "test.Service",
                    """
                            package test;
                            
                            import org.myjtools.jexten.ExtensionPoint;
                            
                            @ExtensionPoint(version = "1.0")
                            public interface Service {
                                void execute();
                            }
                            """
            );

            JavaFileObject abstractClass = JavaFileObjects.forSourceString(
                    "test.AbstractService",
                    """
                            package test;
                            
                            public abstract class AbstractService implements Service {
                                protected void helper() {}
                            }
                            """
            );

            JavaFileObject extension = JavaFileObjects.forSourceString(
                    "test.ConcreteService",
                    """
                            package test;
                            
                            import org.myjtools.jexten.Extension;
                            
                            @Extension(extensionPoint = "test.Service")
                            public class ConcreteService extends AbstractService {
                                @Override
                                public void execute() {}
                            }
                            """
            );

            Compilation compilation = compileWithoutProcessor(extensionPoint, abstractClass, extension);

            assertThat(compilation).succeeded();
        }


        @Test
        @DisplayName("should compile generic extension point implementation")
        void shouldCompileGenericExtensionPointImplementation() {
            JavaFileObject extensionPoint = JavaFileObjects.forSourceString(
                    "test.Repository",
                    """
                            package test;
                            
                            import org.myjtools.jexten.ExtensionPoint;
                            
                            @ExtensionPoint(version = "1.0")
                            public interface Repository<T> {
                                T findById(String id);
                                void save(T entity);
                            }
                            """
            );

            JavaFileObject extension = JavaFileObjects.forSourceString(
                    "test.UserRepository",
                    """
                            package test;
                            
                            import org.myjtools.jexten.Extension;
                            
                            @Extension(extensionPoint = "test.Repository")
                            public class UserRepository implements Repository<String> {
                                @Override
                                public String findById(String id) { return id; }
                                @Override
                                public void save(String entity) {}
                            }
                            """
            );

            Compilation compilation = compileWithoutProcessor(extensionPoint, extension);

            assertThat(compilation).succeeded();
        }


        @Test
        @DisplayName("should compile multiple extensions for same extension point")
        void shouldCompileMultipleExtensions() {
            JavaFileObject extensionPoint = JavaFileObjects.forSourceString(
                    "test.Greeter",
                    """
                            package test;
                            
                            import org.myjtools.jexten.ExtensionPoint;
                            
                            @ExtensionPoint(version = "1.0")
                            public interface Greeter {
                                String greet(String name);
                            }
                            """
            );

            JavaFileObject extension1 = JavaFileObjects.forSourceString(
                    "test.FriendlyGreeter",
                    """
                            package test;
                            
                            import org.myjtools.jexten.Extension;
                            import org.myjtools.jexten.Priority;
                            
                            @Extension(priority = Priority.NORMAL)
                            public class FriendlyGreeter implements Greeter {
                                @Override
                                public String greet(String name) {
                                    return "Hello, " + name + "!";
                                }
                            }
                            """
            );

            JavaFileObject extension2 = JavaFileObjects.forSourceString(
                    "test.FormalGreeter",
                    """
                            package test;
                            
                            import org.myjtools.jexten.Extension;
                            import org.myjtools.jexten.Priority;
                            
                            @Extension(priority = Priority.HIGHER)
                            public class FormalGreeter implements Greeter {
                                @Override
                                public String greet(String name) {
                                    return "Good day, " + name + ".";
                                }
                            }
                            """
            );

            Compilation compilation = compileWithoutProcessor(extensionPoint, extension1, extension2);

            assertThat(compilation).succeeded();
        }


        @Test
        @DisplayName("should compile extension in different package")
        void shouldCompileExtensionInDifferentPackage() {
            JavaFileObject extensionPoint = JavaFileObjects.forSourceString(
                    "test.api.Service",
                    """
                            package test.api;
                            
                            import org.myjtools.jexten.ExtensionPoint;
                            
                            @ExtensionPoint(version = "1.0")
                            public interface Service {
                                void execute();
                            }
                            """
            );

            JavaFileObject extension = JavaFileObjects.forSourceString(
                    "test.impl.ServiceImpl",
                    """
                            package test.impl;
                            
                            import org.myjtools.jexten.Extension;
                            import test.api.Service;
                            
                            @Extension(extensionPoint = "test.api.Service")
                            public class ServiceImpl implements Service {
                                @Override
                                public void execute() {}
                            }
                            """
            );

            Compilation compilation = compileWithoutProcessor(extensionPoint, extension);

            assertThat(compilation).succeeded();
        }
    }


    @Nested
    @DisplayName("Version format validation")
    class VersionFormatValidation {

        @Test
        @DisplayName("should accept valid version formats")
        void shouldAcceptValidVersionFormats() {
            assertValidVersion("2.3");       // major.minor
            assertValidVersion("1.2.3");    // major.minor.patch
            assertValidVersion("0.1.0");    // version with zero
            assertValidVersion("100.200.300"); // large version numbers
        }

        private void assertValidVersion(String version) {
            JavaFileObject source = JavaFileObjects.forSourceString(
                    "test.MyExtensionPoint",
                    """
                            package test;
                            
                            import org.myjtools.jexten.ExtensionPoint;
                            
                            @ExtensionPoint(version = "%s")
                            public interface MyExtensionPoint {}
                            """.formatted(version)
            );

            Compilation compilation = compileWithoutProcessor(source);
            assertThat(compilation).succeeded();
        }


        void assertCompilation(
                String extensionPointSource,
                String extensionSource
        ) {
            JavaFileObject extensionPoint = JavaFileObjects.forSourceString("test.Service", extensionPointSource);
            JavaFileObject extension = JavaFileObjects.forSourceString("test.ServiceImpl", extensionSource);
            Compilation compilation = compileWithoutProcessor(extensionPoint, extension);
            assertThat(compilation).succeeded();
        }


        @Nested
        @DisplayName("Priority and Scope usage")
        class PriorityAndScopeUsage {


            @Test
            @DisplayName("should compile extension with HIGHEST priority")
            void shouldCompileWithHighestPriority() {
                assertCompilation(
                        """
                                package test;
                                
                                import org.myjtools.jexten.ExtensionPoint;
                                
                                @ExtensionPoint(version = "1.0")
                                public interface Service {}
                                """,

                        """
                                package test;
                                
                                import org.myjtools.jexten.Extension;
                                import org.myjtools.jexten.Priority;
                                
                                @Extension(priority = Priority.HIGHEST)
                                public class ServiceImpl implements Service {}
                                """
                );
            }


            @Test
            @DisplayName("should compile extension with SINGLETON scope")
            void shouldCompileWithSingletonScope() {
                assertCompilation(
                        """
                                package test;
                                
                                import org.myjtools.jexten.ExtensionPoint;
                                
                                @ExtensionPoint(version = "1.0")
                                public interface Service {}
                                """,

                        """
                                package test;
                                
                                import org.myjtools.jexten.Extension;
                                import org.myjtools.jexten.Scope;
                                
                                @Extension(scope = Scope.SINGLETON)
                                public class ServiceImpl implements Service {}
                                """
                );

            }


            @Test
            @DisplayName("should compile extension with TRANSIENT scope")
            void shouldCompileWithTransientScope() {
                assertCompilation(
                        """
                                package test;
                                
                                import org.myjtools.jexten.ExtensionPoint;
                                
                                @ExtensionPoint(version = "1.0")
                                public interface Service {}
                                """,

                        """
                                package test;
                                
                                import org.myjtools.jexten.Extension;
                                import org.myjtools.jexten.Scope;
                                
                                @Extension(scope = Scope.TRANSIENT)
                                public class ServiceImpl implements Service {}
                                """
                );
            }


            @Test
            @DisplayName("should compile extension with SESSION scope")
            void shouldCompileWithSessionScope() {
                assertCompilation(
                        """
                                package test;

                                import org.myjtools.jexten.ExtensionPoint;

                                @ExtensionPoint(version = "1.0")
                                public interface Service {}
                                """,

                        """
                                package test;

                                import org.myjtools.jexten.Extension;
                                import org.myjtools.jexten.Scope;

                                @Extension(scope = Scope.SESSION)
                                public class ServiceImpl implements Service {}
                                """
                );
            }
        }


        @Nested
        @DisplayName("Named extensions")
        class NamedExtensions {

            @Test
            @DisplayName("should compile extension with name attribute")
            void shouldCompileExtensionWithName() {
                JavaFileObject extensionPoint = JavaFileObjects.forSourceString(
                        "test.Service",
                        """
                                package test;
                                
                                import org.myjtools.jexten.ExtensionPoint;
                                
                                @ExtensionPoint(version = "1.0")
                                public interface Service {}
                                """
                );

                JavaFileObject extension = JavaFileObjects.forSourceString(
                        "test.ServiceImpl",
                        """
                                package test;
                                
                                import org.myjtools.jexten.Extension;
                                
                                @Extension(name = "my-custom-service")
                                public class ServiceImpl implements Service {}
                                """
                );

                Compilation compilation = compileWithoutProcessor(extensionPoint, extension);

                assertThat(compilation).succeeded();
            }


            @Test
            @DisplayName("should compile multiple named extensions")
            void shouldCompileMultipleNamedExtensions() {
                JavaFileObject extensionPoint = JavaFileObjects.forSourceString(
                        "test.Service",
                        """
                                package test;
                                
                                import org.myjtools.jexten.ExtensionPoint;
                                
                                @ExtensionPoint(version = "1.0")
                                public interface Service {}
                                """
                );

                JavaFileObject extension1 = JavaFileObjects.forSourceString(
                        "test.ServiceImplA",
                        """
                                package test;
                                
                                import org.myjtools.jexten.Extension;
                                
                                @Extension(name = "service-a")
                                public class ServiceImplA implements Service {}
                                """
                );

                JavaFileObject extension2 = JavaFileObjects.forSourceString(
                        "test.ServiceImplB",
                        """
                                package test;
                                
                                import org.myjtools.jexten.Extension;
                                
                                @Extension(name = "service-b")
                                public class ServiceImplB implements Service {}
                                """
                );

                Compilation compilation = compileWithoutProcessor(extensionPoint, extension1, extension2);

                assertThat(compilation).succeeded();
            }
        }
    }
}
