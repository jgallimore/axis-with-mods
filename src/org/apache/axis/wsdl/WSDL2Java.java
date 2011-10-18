/*
 * Copyright 2001-2004 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.axis.wsdl;

import org.apache.axis.constants.Scope;
import org.apache.axis.utils.CLOption;
import org.apache.axis.utils.CLOptionDescriptor;
import org.apache.axis.utils.ClassUtils;
import org.apache.axis.utils.JavaUtils;
import org.apache.axis.utils.Messages;
import org.apache.axis.wsdl.gen.Parser;
import org.apache.axis.wsdl.gen.WSDL2;
import org.apache.axis.wsdl.toJava.Emitter;
import org.apache.axis.wsdl.toJava.NamespaceSelector;

/**
 * Command line interface to the WSDL2Java utility
 */
public class WSDL2Java extends WSDL2 {

    // Define our short one-letter option identifiers.

    /** Field SERVER_OPT */
    protected static final int SERVER_OPT = 's';

    /** Field SKELETON_DEPLOY_OPT */
    protected static final int SKELETON_DEPLOY_OPT = 'S';

    /** Field NAMESPACE_OPT */
    protected static final int NAMESPACE_OPT = 'N';

    /** Field NAMESPACE_FILE_OPT */
    protected static final int NAMESPACE_FILE_OPT = 'f';

    /** Field OUTPUT_OPT */
    protected static final int OUTPUT_OPT = 'o';

    /** Field SCOPE_OPT */
    protected static final int SCOPE_OPT = 'd';

    /** Field TEST_OPT */
    protected static final int TEST_OPT = 't';
    /** Field BUILDFILE_OPT */
    protected static final int BUILDFILE_OPT = 'B';
    /** Field PACKAGE_OPT */
    protected static final int PACKAGE_OPT = 'p';

    /** Field ALL_OPT */
    protected static final int ALL_OPT = 'a';

    /** Field TYPEMAPPING_OPT */
    protected static final int TYPEMAPPING_OPT = 'T';

    /** Field FACTORY_CLASS_OPT */
    protected static final int FACTORY_CLASS_OPT = 'F';

    /** Field HELPER_CLASS_OPT */
    protected static final int HELPER_CLASS_OPT = 'H';

    /** Field USERNAME_OPT */
    protected static final int USERNAME_OPT = 'U';

    /** Field PASSWORD_OPT */
    protected static final int PASSWORD_OPT = 'P';

    protected static final int CLASSPATH_OPT = 'X';

    /** Field bPackageOpt */
    protected boolean bPackageOpt = false;

    /** Field namespace include */
    protected static final int NS_INCLUDE_OPT = 'i';
    
	/** Filed namespace exclude */
	protected static final int NS_EXCLUDE_OPT = 'x';
	
	/** Field IMPL_CLASS_OPT */
	protected static final int IMPL_CLASS_OPT = 'c';

    /** Field ALLOW_INVALID_URL_OPT */
    protected static final int ALLOW_INVALID_URL_OPT = 'u';
    
    /** Wrap arrays option */
    protected static final int WRAP_ARRAYS_OPT = 'w';

    /** Field emitter */
    private Emitter emitter;
    
    /**
     * Define the understood options. Each CLOptionDescriptor contains:
     * - The "long" version of the option. Eg, "help" means that "--help" will
     * be recognised.
     * - The option flags, governing the option's argument(s).
     * - The "short" version of the option. Eg, 'h' means that "-h" will be
     * recognised.
     * - A description of the option for the usage message
     */

    protected static final CLOptionDescriptor[] options =
            new CLOptionDescriptor[]{
                new CLOptionDescriptor("server-side",
                        CLOptionDescriptor.ARGUMENT_DISALLOWED,
                        SERVER_OPT, Messages.getMessage("optionSkel00")),
                new CLOptionDescriptor("skeletonDeploy",
                        CLOptionDescriptor.ARGUMENT_REQUIRED,
                        SKELETON_DEPLOY_OPT,
                        Messages.getMessage("optionSkeletonDeploy00")),
                new CLOptionDescriptor("NStoPkg",
                        CLOptionDescriptor.DUPLICATES_ALLOWED
                        + CLOptionDescriptor.ARGUMENTS_REQUIRED_2,
                        NAMESPACE_OPT,
                        Messages.getMessage("optionNStoPkg00")),
                new CLOptionDescriptor("fileNStoPkg",
                        CLOptionDescriptor.ARGUMENT_REQUIRED,
                        NAMESPACE_FILE_OPT,
                        Messages.getMessage("optionFileNStoPkg00")),
                new CLOptionDescriptor("package", CLOptionDescriptor.ARGUMENT_REQUIRED,
                        PACKAGE_OPT,
                        Messages.getMessage("optionPackage00")),
                new CLOptionDescriptor("output", CLOptionDescriptor.ARGUMENT_REQUIRED,
                        OUTPUT_OPT,
                        Messages.getMessage("optionOutput00")),
                new CLOptionDescriptor("deployScope",
                        CLOptionDescriptor.ARGUMENT_REQUIRED, SCOPE_OPT,
                        Messages.getMessage("optionScope00")),
                new CLOptionDescriptor("testCase",
                        CLOptionDescriptor.ARGUMENT_DISALLOWED,
                        TEST_OPT, Messages.getMessage("optionTest00")),
                new CLOptionDescriptor("all", CLOptionDescriptor.ARGUMENT_DISALLOWED,
                        ALL_OPT, Messages.getMessage("optionAll00")),
                new CLOptionDescriptor("typeMappingVersion",
                        CLOptionDescriptor.ARGUMENT_REQUIRED,
                        TYPEMAPPING_OPT,
                        Messages.getMessage("optionTypeMapping00")),
                new CLOptionDescriptor("factory", CLOptionDescriptor.ARGUMENT_REQUIRED,
                        FACTORY_CLASS_OPT,
                        Messages.getMessage("optionFactory00")),
                new CLOptionDescriptor("helperGen",
                        CLOptionDescriptor.ARGUMENT_DISALLOWED,
                        HELPER_CLASS_OPT,
                        Messages.getMessage("optionHelper00")),
                new CLOptionDescriptor("buildFile", CLOptionDescriptor.ARGUMENT_DISALLOWED,
                        BUILDFILE_OPT,
                        Messages.getMessage("optionBuildFile00")),        
                new CLOptionDescriptor("user", CLOptionDescriptor.ARGUMENT_REQUIRED,
                        USERNAME_OPT,
                        Messages.getMessage("optionUsername")),
                new CLOptionDescriptor("password",
                        CLOptionDescriptor.ARGUMENT_REQUIRED,
                        PASSWORD_OPT,
                        Messages.getMessage("optionPassword")),
                new CLOptionDescriptor("classpath",
                        CLOptionDescriptor.ARGUMENT_OPTIONAL,
                        CLASSPATH_OPT,
                        Messages.getMessage("optionClasspath")),
                new CLOptionDescriptor("nsInclude",
                        CLOptionDescriptor.DUPLICATES_ALLOWED
                        + CLOptionDescriptor.ARGUMENT_REQUIRED,
                        NS_INCLUDE_OPT,
                        Messages.getMessage("optionNSInclude")),
				new CLOptionDescriptor("nsExclude",
						CLOptionDescriptor.DUPLICATES_ALLOWED
						+ CLOptionDescriptor.ARGUMENT_REQUIRED,
						NS_EXCLUDE_OPT,
						Messages.getMessage("optionNSExclude")),
				new CLOptionDescriptor("implementationClassName",
						CLOptionDescriptor.ARGUMENT_REQUIRED,
						IMPL_CLASS_OPT,
						Messages.getMessage("implementationClassName")),
                new CLOptionDescriptor("allowInvalidURL", CLOptionDescriptor.ARGUMENT_DISALLOWED,
                        ALLOW_INVALID_URL_OPT, Messages.getMessage("optionAllowInvalidURL")),
                new CLOptionDescriptor("wrapArrays",
                                       CLOptionDescriptor.ARGUMENT_OPTIONAL,
                                       WRAP_ARRAYS_OPT,
                                       Messages.getMessage("optionWrapArrays")),
                };

    /**
     * Instantiate a WSDL2Java emitter.
     */
    protected WSDL2Java() {

        // emitter is the same as the parent's parser variable.  Just cast it
        // here once so we don't have to cast it every time we use it.
        emitter = (Emitter) parser;

        addOptions(options);
    }    // ctor

    /**
     * Instantiate an extension of the Parser
     * 
     * @return 
     */
    protected Parser createParser() {
        return new Emitter();
    }    // createParser
    
    /**
     * Parse an option
     * 
     * @param option is the option
     */
    protected void parseOption(CLOption option) {

        switch (option.getId()) {

            case FACTORY_CLASS_OPT:
                emitter.setFactory(option.getArgument());
                break;

            case HELPER_CLASS_OPT:
                emitter.setHelperWanted(true);
                break;

            case SKELETON_DEPLOY_OPT:
                emitter.setSkeletonWanted(
                        JavaUtils.isTrueExplicitly(option.getArgument(0)));

                // --skeletonDeploy assumes --server-side, so fall thru
            case SERVER_OPT:
                emitter.setServerSide(true);
                break;

            case NAMESPACE_OPT:
                String namespace = option.getArgument(0);
                String packageName = option.getArgument(1);

                emitter.getNamespaceMap().put(namespace, packageName);
                break;

            case NAMESPACE_FILE_OPT:
                emitter.setNStoPkg(option.getArgument());
                break;

            case PACKAGE_OPT:
                bPackageOpt = true;

                emitter.setPackageName(option.getArgument());
                break;

            case OUTPUT_OPT:
                emitter.setOutputDir(option.getArgument());
                break;

            case SCOPE_OPT:
                String arg = option.getArgument();

                // Provide 'null' default, prevents logging internal error.
                // we have something different to report here.
                Scope scope = Scope.getScope(arg, null);

                if (scope != null) {
                    emitter.setScope(scope);
                } else {
                    System.err.println(Messages.getMessage("badScope00", arg));
                }
                break;

            case TEST_OPT:
                emitter.setTestCaseWanted(true);
                break;
            case BUILDFILE_OPT:
                emitter.setBuildFileWanted(true);
                break;
            case ALL_OPT:
                emitter.setAllWanted(true);
                break;

            case TYPEMAPPING_OPT:
                String tmValue = option.getArgument();

                if (tmValue.equals("1.0")) {
                    emitter.setTypeMappingVersion("1.0");
                } else if (tmValue.equals("1.1")) {
                        emitter.setTypeMappingVersion("1.1");
                } else if (tmValue.equals("1.2")) {
                    emitter.setTypeMappingVersion("1.2");
                } else if (tmValue.equals("1.3")) {
                    emitter.setTypeMappingVersion("1.3");
                } else {
                    System.out.println(
                            Messages.getMessage("badTypeMappingOption00"));
                }
                break;

            case USERNAME_OPT:
                emitter.setUsername(option.getArgument());
                break;

            case PASSWORD_OPT:
                emitter.setPassword(option.getArgument());
                break;

            case CLASSPATH_OPT:
                ClassUtils.setDefaultClassLoader(ClassUtils.createClassLoader(
                        option.getArgument(),
                        this.getClass().getClassLoader()));
                break;

            case NS_INCLUDE_OPT:
                NamespaceSelector include = new NamespaceSelector();
                include.setNamespace(option.getArgument());
                emitter.getNamespaceIncludes().add(include);
                break;
            case NS_EXCLUDE_OPT:
                NamespaceSelector exclude = new NamespaceSelector();
                exclude.setNamespace(option.getArgument());
                emitter.getNamespaceExcludes().add(exclude);
                break;

			case IMPL_CLASS_OPT:
				emitter.setImplementationClassName(option.getArgument());
				break;

            case ALLOW_INVALID_URL_OPT:
                emitter.setAllowInvalidURL(true);
                break;

            case WRAP_ARRAYS_OPT:
                emitter.setWrapArrays(true);
                break;

            default :
                super.parseOption(option);
        }
    }    // parseOption

    /**
     * validateOptions
     * This method is invoked after the options are set to validate
     * the option settings.
     */
    protected void validateOptions() {

        super.validateOptions();

        // validate argument combinations
        if (emitter.isSkeletonWanted() && !emitter.isServerSide()) {
            System.out.println(Messages.getMessage("badSkeleton00"));
            printUsage();
        }

        if (!emitter.getNamespaceMap().isEmpty() && bPackageOpt) {
            System.out.println(Messages.getMessage("badpackage00"));
            printUsage();
        }
    }    // validateOptions

    /**
     * Main
     * Run the WSDL2Java emitter with the specified command-line arguments
     * 
     * @param args command-line arguments
     */
    public static void main(String args[]) {

        WSDL2Java wsdl2java = new WSDL2Java();

        wsdl2java.run(args);
    }
}
