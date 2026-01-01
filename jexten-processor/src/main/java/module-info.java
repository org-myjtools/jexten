/**
 * Annotation processor module for the JExten extension framework.
 * <p>
 * This module provides compile-time validation and code generation for JExten extensions.
 * The processor validates:
 * <ul>
 *   <li>Correct usage of {@code @Extension} and {@code @ExtensionPoint} annotations</li>
 *   <li>Proper module-info.java declarations (exports, opens, provides, uses)</li>
 *   <li>Version format compliance for extension point versioning</li>
 *   <li>Type hierarchy correctness (extensions must implement their extension points)</li>
 * </ul>
 * <p>
 * The processor also generates META-INF metadata files for runtime extension discovery.
 *
 * @see org.myjtools.jexten.Extension
 * @see org.myjtools.jexten.ExtensionPoint
 */
module org.myjtools.jexten.processor {

    // Java Compiler API for annotation processing support
    // Provides access to javax.annotation.processing and javax.lang.model packages
    requires java.compiler;

    // Core JExten module for access to annotation definitions
    requires org.myjtools.jexten;

    // Note: This module does not export any packages because the processor
    // is discovered via META-INF/services/javax.annotation.processing.Processor
    // and is not intended to be used as a library dependency
}
