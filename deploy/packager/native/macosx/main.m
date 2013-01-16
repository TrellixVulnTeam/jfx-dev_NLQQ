/*
 * Copyright 2012, Oracle and/or its affiliates. All rights reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

#import <Cocoa/Cocoa.h>
#include <dlfcn.h>
#include <pthread.h>
//#include <jni.h>

//essential imports from jni.h
#define JNICALL
typedef unsigned char   jboolean;
#if defined(__LP64__) && __LP64__ /* for -Wundef */
typedef int jint;
#else
typedef long jint;
#endif
////////////////////////// end of imports from jni.h


#define JAVA_LAUNCH_ERROR "JavaLaunchError"

#define JVM_RUNTIME_KEY "JVMRuntime"
#define JVM_MAIN_CLASS_NAME_KEY "JVMMainClassName"
#define JVM_MAIN_JAR_NAME_KEY "JVMMainJarName"
#define JVM_OPTIONS_KEY "JVMOptions"
#define JVM_ARGUMENTS_KEY "JVMArguments"
#define JVM_CLASSPATH_KEY "JVMAppClasspath"

#define LIBJLI_DYLIB "/Library/Internet Plug-Ins/JavaAppletPlugin.plugin/Contents/Home/lib/jli/libjli.dylib"

typedef int (JNICALL *JLI_Launch_t)(int argc, char ** argv,
                                    int jargc, const char** jargv,
                                    int appclassc, const char** appclassv,
                                    const char* fullversion,
                                    const char* dotversion,
                                    const char* pname,
                                    const char* lname,
                                    jboolean javaargs,
                                    jboolean cpwildcard,
                                    jboolean javaw,
                                    jint ergo);

int launch(int appArgc, char *appArgv[]);

int main(int argc, char *argv[]) {
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];

    int result;
    @try {
        launch(argc, argv);
        result = 0;
    } @catch (NSException *exception) {
        NSLog(@"%@: %@", exception, [exception callStackSymbols]);
        result = 1;
    }

    [pool drain];

    return result;
}

int launch(int appArgc, char *appArgv[]) {
    // Get the main bundle
    NSBundle *mainBundle = [NSBundle mainBundle];

    char *commandName = appArgv[0];

    // Set the working directory to the user's home directory
    chdir([NSHomeDirectory() UTF8String]);

    // Get the main bundle's info dictionary
    NSDictionary *infoDictionary = [mainBundle infoDictionary];

    // Locate the JLI_Launch() function
    NSString *runtime = [infoDictionary objectForKey:@JVM_RUNTIME_KEY];

    JLI_Launch_t jli_LaunchFxnPtr;
    if ([runtime length] != 0) { //missing key or empty value
        NSString *runtimePath = [[[NSBundle mainBundle] builtInPlugInsPath] stringByAppendingPathComponent:runtime];
        NSString *libjliPath = [runtimePath stringByAppendingPathComponent:@"Contents/Home/jre/lib/jli/libjli.dylib"];

        if ([[NSFileManager defaultManager] fileExistsAtPath:libjliPath]) {
            const char *jliPath = [libjliPath fileSystemRepresentation];
            void *libJLI = dlopen(jliPath, RTLD_LAZY);
            if (libJLI != NULL) {
                jli_LaunchFxnPtr = dlsym(libJLI, "JLI_Launch");
        }
        }
    } else {
        void *libJLI = dlopen(LIBJLI_DYLIB, RTLD_LAZY);
        if (libJLI != NULL) {
            jli_LaunchFxnPtr = dlsym(libJLI, "JLI_Launch");
        }
    }

    if (jli_LaunchFxnPtr == NULL) {
        [NSException raise:@JAVA_LAUNCH_ERROR format:@"Could not get function pointer for JLI_Launch."];
    }

    // Get the main class name
    NSString *mainClassName = [infoDictionary objectForKey:@JVM_MAIN_CLASS_NAME_KEY];
    if (mainClassName == nil) {
        [NSException raise:@JAVA_LAUNCH_ERROR format:@"%@ is required.", @JVM_MAIN_CLASS_NAME_KEY];
    }

    // Get the main jar name
    NSString *mainJarName = [infoDictionary objectForKey:@JVM_MAIN_JAR_NAME_KEY];
    if (mainJarName == nil) {
        [NSException raise:@JAVA_LAUNCH_ERROR format:@"%@ is required.", @JVM_MAIN_JAR_NAME_KEY];
    }

    // Set the class path
    // Assume we are given main executable jar file that knows how to set classpath
    //  and launch the app (i.e. it works for doubleclick on jar)
    NSString *mainBundlePath = [mainBundle bundlePath];
    NSString *javaPath = [mainBundlePath stringByAppendingString:@"/Contents/Java"];
    NSMutableString *classPath = [NSMutableString stringWithFormat:@"-Djava.class.path=%@/%@",
         javaPath, mainJarName];

    NSString *extraClasspath = [infoDictionary objectForKey:@JVM_CLASSPATH_KEY];
    if ([extraClasspath length] > 0) { //unless key missing or has empty value
       NSArray *elements = [extraClasspath componentsSeparatedByString:@" "];
       for (NSString *file in elements) {
          if ([file length] > 0) {
             [classPath appendFormat:@":%@/%@", javaPath, file];
          }
       }
    }
    // Set the library path
    NSString *libraryPath = [NSString stringWithFormat:@"-Djava.library.path=%@/Contents/Java", mainBundlePath];

    // Get the VM options
    NSArray *options = [infoDictionary objectForKey:@JVM_OPTIONS_KEY];
    if (options == nil) {
        options = [NSArray array];
    }

    // Get the application arguments
    NSArray *arguments = [infoDictionary objectForKey:@JVM_ARGUMENTS_KEY];
    if (arguments == nil) {
        arguments = [NSArray array];
    }

    // Initialize the arguments to JLI_Launch()
    //
    // On Mac OS X we spawn a new thread that actually starts the JVM. This
    // new thread simply re-runs main(argc, argv). Therefore we do not want
    // to add new args if we are still in the original main thread ot we
    // will treat them as command line args provided by the user ...
    // Only propagate original set of args first time
    int mainThread = (pthread_main_np() == 1);
    int argc;
    if (!mainThread) {
        argc = 1 + [options count] + 2 + 1 +
               (appArgc > 1 ? (appArgc - 1) : [arguments count]);
    } else {
        argc = 1 + (appArgc > 1 ? (appArgc - 1) : 0);
    }

    // argv[argc] == NULL by convention, so allow one extra space
    // for the null termination.
    char *argv[argc + 1];

    int i = 0;
    argv[i++] = strdup(commandName);

    if (!mainThread) {
        argv[i++] = strdup([classPath UTF8String]);
        argv[i++] = strdup([libraryPath UTF8String]);

        for (NSString *option in options) {
            argv[i++] = strdup([option UTF8String]);
        }

        argv[i++] = strdup([mainClassName UTF8String]);

        //command line arguments override plist
        if (appArgc > 1) {
            for (int j=1; j<appArgc; j++) {
               argv[i++] = strdup(appArgv[j]);
            }
        } else {
            for (NSString *argument in arguments) {
               argv[i++] = strdup([argument UTF8String]);
            }
        }
    } else {
        for (int j=1; j<appArgc; j++) {
           argv[i++] = strdup(appArgv[j]);
        }
    }

    argv[i] = NULL;

    // Invoke JLI_Launch()
    return jli_LaunchFxnPtr(argc, argv,
                            0, NULL,
                            0, NULL,
                            "",
                            "",
                            "java",
                            "java",
                            FALSE,
                            FALSE,
                            FALSE,
                            0);
}
