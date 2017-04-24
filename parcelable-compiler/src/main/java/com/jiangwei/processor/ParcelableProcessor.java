package com.jiangwei.processor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic;

import com.google.auto.service.AutoService;
import com.jiangwei.annotation.Parcelable;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

/**
 * author: jiangwei18 on 17/4/24 00:16 email: jiangwei18@baidu.com Hi: jwill金牛
 */
@AutoService(Processor.class)
public class ParcelableProcessor extends AbstractProcessor {

    private Messager mMessager;
    private Map<String, ArrayList<VariableElement>> mVarMap = new HashMap<>();
    private Map<String, TypeSpec.Builder> typeSpecs = new HashMap<>();
    private Map<String, MethodSpec.Builder> constructorParameters = new HashMap<>();
    private Map<String, MethodSpec.Builder> builderMethodWriteToParcels = new HashMap<>();

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        mMessager = processingEnvironment.getMessager();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> types = new HashSet<>();
        types.add(Parcelable.class.getCanonicalName());
        return types;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        // process方法可能会执行多次,每次清空一下mVarMap
        mVarMap.clear();
        typeSpecs.clear();
        constructorParameters.clear();
        builderMethodWriteToParcels.clear();
        mMessager.printMessage(Diagnostic.Kind.NOTE, "4");
        Set<? extends Element> elements = roundEnvironment.getElementsAnnotatedWith(Parcelable.class);
        // 收集信息
        for (Element element : elements) {
            if (element.getKind() != ElementKind.FIELD) {
                mMessager.printMessage(Diagnostic.Kind.ERROR, "element should be used on field", element);
                return true;
            }
            Parcelable annotation = element.getAnnotation(Parcelable.class);
            VariableElement variableElement = (VariableElement) element;
            TypeElement typeElement = (TypeElement) variableElement.getEnclosingElement();
            String classFullName = typeElement.getQualifiedName().toString();
            ArrayList<VariableElement> elementList = mVarMap.get(classFullName);
            if (elementList == null) {
                elementList = new ArrayList<>();
            }
            if (!elementList.contains(variableElement)) {
                elementList.add(variableElement);
                mVarMap.put(classFullName, elementList);
            }
        }
        for (String classFullName : mVarMap.keySet()) {
            ArrayList<VariableElement> variableElements = mVarMap.get(classFullName);
            TypeSpec.Builder builder = null;
            String packageName = null;
            MethodSpec.Builder constructorParameter = null;
            MethodSpec.Builder builderMethodWriteToParcel = null;
            for (VariableElement variableElement : variableElements) {
                String thatClassFullName = variableElement.asType().toString();
                String[] strings = thatClassFullName.split("\\.");
                String thatClassName = strings[strings.length - 1];
                String thatPackageName = thatClassFullName.replace("." + thatClassName, "");
                TypeElement typeElement = (TypeElement) variableElement.getEnclosingElement();
                PackageElement packageElement = processingEnv.getElementUtils().getPackageOf(variableElement);
                packageName = packageElement.getQualifiedName().toString();
                String className = getClassName(typeElement, packageName);
                ClassName interfaceName = ClassName.get("android.os", "Parcelable");
                if (!typeSpecs.containsKey(className)) {
                    // 第一次创建
                    String s = judgeFieldType(variableElement);
                    if (s == null) {
                        mMessager.printMessage(Diagnostic.Kind.ERROR, "暂时不支持这种数据格式", variableElement);
                        return true;
                    }
                    builder = TypeSpec.classBuilder(className + "$$Parcelable").addModifiers(Modifier.PUBLIC)
                            .addSuperinterface(interfaceName);
                    // describeContents
                    MethodSpec.Builder builderMethodContents =
                            MethodSpec.methodBuilder("describeContents").addModifiers(Modifier.PUBLIC)
                                    .addAnnotation(Override.class).returns(TypeName.INT).addStatement("return 0");
                    builder.addMethod(builderMethodContents.build());
                    // writeToParcel
                    builderMethodWriteToParcel = getBuilderWriteToParcel(className);
                    builderMethodWriteToParcel.addModifiers(Modifier.PUBLIC).addAnnotation(Override.class)
                            .returns(TypeName.VOID).addParameter(ClassName.get("android.os", "Parcel"), "dest")
                            .addParameter(TypeName.INT, "flags");
                    builderWriteToParcelStatement(builderMethodWriteToParcel, variableElement, s);
                    // constructorParameter
                    constructorParameter = getBuilderConstructorParameter(className);
                    constructorParameter.addModifiers(Modifier.PROTECTED)
                            .addParameter(ClassName.get("android.os", "Parcel"), "in");
                    ConstructorParameterAddStatement(constructorParameter, variableElement, s);
                    // constructor()
                    MethodSpec constructor = MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC).build();
                    builder.addMethod(constructor);

                    FieldSpec field = null;
                    if (s.equals("Parcelable")) {
                        field = FieldSpec
                                .builder(ClassName.get(thatPackageName, thatClassName + "$$Parcelable"),
                                        variableElement.getSimpleName().toString())
                                .addModifiers(Modifier.PUBLIC).build();
                    } else {
                        field = FieldSpec
                                .builder(ClassName.get(variableElement.asType()),
                                        variableElement.getSimpleName().toString())
                                .addModifiers(Modifier.PUBLIC).build();
                    }
                    builder.addField(field);
                    // 创建Creator
                    ClassName type = ClassName.get(packageName, className + "$$Parcelable");
                    ClassName creatorName = ClassName.get("android.os", "Parcelable.Creator");
                    TypeName creatorAddType = ParameterizedTypeName.get(creatorName, type);
                    FieldSpec.Builder creator = FieldSpec.builder(creatorAddType, "CREATOR")
                            .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL).initializer(
                                    "new Creator<$L>() {\n" + "        @Override\n"
                                            + "        public $L createFromParcel(Parcel source) {\n"
                                            + "            return new $L(source);\n" + "        }\n" + "\n"
                                            + "        @Override\n" + "        public $L[] newArray(int size) {\n"
                                            + "            return new $L[size];\n" + "        }\n" + "    }",
                                    className + "$$Parcelable", className + "$$Parcelable", className + "$$Parcelable",
                                    className + "$$Parcelable", className + "$$Parcelable");
                    builder.addField(creator.build());
                    typeSpecs.put(className, builder);
                } else {
                    String s = judgeFieldType(variableElement);
                    if (s == null) {
                        mMessager.printMessage(Diagnostic.Kind.ERROR, "暂时不支持这种数据格式", variableElement);
                        return true;
                    }
                    FieldSpec field = null;
                    if (s.equals("Parcelable")) {
                        field = FieldSpec
                                .builder(ClassName.get(thatPackageName, thatClassName + "$$Parcelable"),
                                        variableElement.getSimpleName().toString())
                                .addModifiers(Modifier.PUBLIC).build();
                    } else {
                        field = FieldSpec
                                .builder(ClassName.get(variableElement.asType()),
                                        variableElement.getSimpleName().toString())
                                .addModifiers(Modifier.PUBLIC).build();
                    }

                    // constructorParameter
                    builderMethodWriteToParcel = getBuilderWriteToParcel(className);
                    builderWriteToParcelStatement(builderMethodWriteToParcel, variableElement, s);
                    constructorParameter = getBuilderConstructorParameter(className);
                    ConstructorParameterAddStatement(constructorParameter, variableElement, s);
                    builder = typeSpecs.get(className);
                    builder.addField(field);
                }
            }
            builder.addMethod(constructorParameter.build());
            builder.addMethod(builderMethodWriteToParcel.build());
            if (packageName == null) {
                mMessager.printMessage(Diagnostic.Kind.ERROR, "packageName == null can't be generated JavaFile");
                return true;
            }
            if (builder == null) {
                mMessager.printMessage(Diagnostic.Kind.ERROR, "TypeSpec Builder == null can't be generated JavaFile");
                return true;
            }
            JavaFile javaFile = JavaFile.builder(packageName, builder.build()).build();
            try {
                javaFile.writeTo(processingEnv.getFiler());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    private void ConstructorParameterAddStatement(MethodSpec.Builder constructorParameter, VariableElement variableElement, String s) {
        if (s.equals("Parcelable")) {
            constructorParameter.addStatement("this.$L = in.read$L($L)", variableElement.getSimpleName(), s,
                    variableElement.asType().toString() + ".class.getClassLoader()");
        } else {
            constructorParameter.addStatement("this.$L = in.read$L($L)", variableElement.getSimpleName(), s,
                    "");
        }
    }

    private MethodSpec.Builder getBuilderConstructorParameter(String className) {
        MethodSpec.Builder constructorParameter;
        if (!constructorParameters.containsKey(className)) {
            constructorParameter = MethodSpec.constructorBuilder();
            constructorParameters.put(className, constructorParameter);
        } else {
            constructorParameter = constructorParameters.get(className);
        }
        return constructorParameter;
    }

    private void builderWriteToParcelStatement(MethodSpec.Builder builderMethodWriteToParcel, VariableElement variableElement, String s) {
        if (s.equals("Parcelable")) {
            builderMethodWriteToParcel.addStatement("dest.write$L(this.$L, flags)", s,
                    variableElement.getSimpleName());
        } else {
            builderMethodWriteToParcel.addStatement("dest.write$L(this.$L)", s,
                    variableElement.getSimpleName());
        }
    }

    private MethodSpec.Builder getBuilderWriteToParcel(String className) {
        MethodSpec.Builder builderMethodWriteToParcel;
        if (!builderMethodWriteToParcels.containsKey(className)) {
            builderMethodWriteToParcel = MethodSpec.methodBuilder("writeToParcel");
            builderMethodWriteToParcels.put(className, builderMethodWriteToParcel);
        } else {
            builderMethodWriteToParcel = builderMethodWriteToParcels.get(className);
        }
        return builderMethodWriteToParcel;
    }

    private String judgeFieldType(VariableElement variableElement) {
        // 仅支持数据的基本类型,如需其他
        String type = variableElement.asType().toString();
        // String
        if (type.contains("String")) {
            return "String";
        } else if (type.contains("int")) {
            // int
            return "Int";
        } else if (type.contains("double")) {
            // double
            return "Double";
        } else if (type.contains("float")) {
            // float
            return "Float";
        } else if (type.contains("Bundle")) {
            // Bundle
            return "Bundle";
        } else if (type.contains("byte")) {
            // Byte
            return "Byte";
        } else if (type.contains("boolean")) {
            return null;
        } else {
            // Parcelable
            return "Parcelable";
            // this.book = in.readParcelable(Book.class.getClassLoader());
        }
    }

    public String getClassName(TypeElement type, String packageName) {
        int packageLen = packageName.length() + 1;
        return type.getQualifiedName().toString().substring(packageLen).replace('.', '$');
    }
}
