package com.bjsdm.my_processor;

import com.bjsdm.my_annotations.MyBindView;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

public class MyBindingProcessor extends AbstractProcessor {
    Filer filer;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        filer = processingEnvironment.getFiler();
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        //获取全部的类
        for (Element element : roundEnvironment.getRootElements()) {
            //获取类的包名
            String packageStr = element.getEnclosingElement().toString();
            //获取类的名字
            String classStr = element.getSimpleName().toString();
            //构建新的类的名字：原类名 + Binding
            ClassName className = ClassName.get(packageStr, classStr + "Binding");
            //构建新的类的构造方法
            MethodSpec.Builder constructorBuilder = MethodSpec.constructorBuilder()
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(ClassName.get(packageStr, classStr), "activity");
            //判断是否要生成新的类，假如该类里面 MyBindView 注解，那么就不需要新生成
            boolean hasBuild = false;

            //获取类的元素，例如类的成员变量、方法、内部类等
            for (Element enclosedElement : element.getEnclosedElements()) {
                //仅获取成员变量
                if (enclosedElement.getKind() == ElementKind.FIELD) {
                    //判断是否被 MyBindView 注解
                    MyBindView bindView = enclosedElement.getAnnotation(MyBindView.class);
                    if (bindView != null) {
                        //设置需要生成类
                        hasBuild = true;
                        //在构造方法中加入代码
                        constructorBuilder.addStatement("activity.$N = activity.findViewById($L)",
                                enclosedElement.getSimpleName(), bindView.value());
                    }
                }
            }
            //是否需要生成
            if (hasBuild) {
                try {
                    //构建新的类
                    TypeSpec builtClass = TypeSpec.classBuilder(className)
                            .addModifiers(Modifier.PUBLIC)
                            .addMethod(constructorBuilder.build())
                            .build();
                    //生成 Java 文件
                    JavaFile.builder(packageStr, builtClass)
                            .build().writeTo(filer);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        //只支持 MyBindView 注解
        return Collections.singleton(MyBindView.class.getCanonicalName());
    }
}