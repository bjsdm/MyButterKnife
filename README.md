## ButterKnife的应用

如题所示，这篇文章主要讲解手把手教学写个简易版 ButterKnife，所以，还是先看看 ButterKnife 怎么使用，不过大多数人都用过，就不详细介绍，若有需要，请参考：

> https://github.com/JakeWharton/butterknife

虽然有人觉得，在 kotlin 中，使用 KTX 工具或者使用 MVVM，已经很少使用 ButterKnife 了，所以觉得没必要去研究 ButterKnife，但是我们学习并非是为了使用，而是为了清楚其原理及构造，就如本文，**只是借手写 ButterKnife 去了解如何实现注解、annotationProcessor 的使用等**。醉翁之意不在酒，在乎山水之间。

好了，不多说，开始我的表演：

<img src="https://img-blog.csdnimg.cn/20210325151325453.jpg" width = "150" >

在`build.gradle`添加依赖：

```
android {
  ...
  // Butterknife requires Java 8.
  compileOptions {
    sourceCompatibility JavaVersion.VERSION_1_8
    targetCompatibility JavaVersion.VERSION_1_8
  }
}
dependencies {
  implementation 'com.jakewharton:butterknife:10.2.3'
  annotationProcessor 'com.jakewharton:butterknife-compiler:10.2.3'
}
```

在`MainActivity`中书写：

```
public class MainActivity extends AppCompatActivity {
    @BindView(R.id.tv_content)
    TextView tvContent;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
		···
        ButterKnife.bind(this);
        tvContent.setText("修改成功！");
    }
}
```

em...运行结果就不发，反正是成功的😂。

稍微拆解下，ButterKnife 的代码使用为两步：

- `@BindView(R.id.tv_content)`：注解声明
- `ButterKnife.bind(this);`：注解解析

## 注解声明

首先，我们新建一个注解`MyBindView.java`：

```
public @interface MyBindView {
}
```

注解其实就是一种标签说明，并没有实际的特殊代码作用，就类似于注释：

- 单行注释 // 这是单注释
- 多行注释 /*这是多行注释*/
- Javadoc注释 /**这是javadoc注释*/

但是不同于注释在字节码阶段就被擦除，注解可以选择保留到什么阶段，和附带了其它功能：

- @Retention
	- RetentionPolicy.SOURCE：仅保留在源码阶段，其它阶段被擦除
	- RetentionPolicy.CLASS：保留到字节码阶段，字节码之后的阶段被擦除
	- RetentionPolicy.RUNTIME：保留到运行阶段，也就是在代码运行的时候，还能读取到注解
- @Documented
	- 标记注解能够包含在用户说明文档中
- @Target
	- 限制可注解的对象，如只能注解成员变量、方法、类等，详情请查看 ElementType 类
- @Inherited
	- 标识该注解可继承。如 A 有 @Inherited 和 @Retention、@Target，B 没有注解，但是 B extend A，那么 B 就会继承 A 的注解

根据上面的说明，我们就可以完善下注解。

因为我们的注解在运行阶段要用到，并且只能注解成员变量，所以，可以写成这样：

```
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MyBindView {
}
```

然后就可以使用了：

```
    @MyBindView
    TextView tvContent;
```

不过，这样是不够的，我们不知道要绑定哪个 id，所以，我们要在注解上加上变量，这样赋值过去，我们就知道为哪个 id 绑定了：

```
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MyBindView {
    int viewId();
}
```

嗯？为什么 viewId 后面有个 () ？这只能说是写法的规定，先记住。

我们再看看使用：

```
    @MyBindView(viewId = R.id.tv_content)
    TextView tvContent;
```

赋值成功！

<img src="https://img-blog.csdnimg.cn/20210325163602989.jpg" width = "100" >

em...多出了 viewId，但是去掉又会报错？为什么 ButterKnife 不需要写，而我需要？

这个因为要把 viewId 改为 value，假如名称为 value 的话，编译器会自动帮我们赋值，所以我们要稍微改下：

```
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MyBindView {
    int value();
}
```

```
    @MyBindView(R.id.tv_content)
    TextView tvContent;
```

好了，注解声明的功能已经完成了。我们再加把劲，把注解解析也干掉。

<img src="https://img-blog.csdnimg.cn/20210325164827608.jpg" width = "150" >


## 注解解析
ButterKnife 是通过以下代码开始解析的：

```
ButterKnife.bind(this);
```

那我们也新建个类：

```
public class MyButterKnife {
    public static void bind(Activity activity){
    }
}
```

OK，完结撒花~~

当然不是，我们要在 bind() 方法里面写控件的绑定代码。

首先，我们先理清逻辑，只要逻辑没问题，代码实现就不在话下：

- 获取该 Activity 的全部成员变量
- 判断这个成员变量是否被 MyBindView 进行注解
- 注解符合的话，就对该成员变量进行 findViewById 赋值

具体代码如下：

```
    public static void bind(Activity activity) {
        //获取该 Activity 的全部成员变量
        for (Field field : activity.getClass().getDeclaredFields()) {
            //判断这个成员变量是否被 MyBindView 进行注解
            MyBindView myBindView = field.getAnnotation(MyBindView.class);
            if (myBindView != null) {
                try {
                    //注解符合的话，就对该成员变量进行 findViewById 赋值
                    //相当于 field = activity.findViewById(myBindView.value())
                    field.set(activity, activity.findViewById(myBindView.value()));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }
```

运行！

我看了下手机，运行是没问题的！

<img src="https://img-blog.csdnimg.cn/20210325200242185.jpg" width = "150" >

虽然，上面的功能实现上是没有问题的，但是，每个控件的绑定都要依靠反射，这太耗性能，一个还好，但是正常的 Activity 都不止一个 View，随着 View 的增加，执行的时间越长，所以，我们必须寻找新的出路，那就是`AnnotationProcessor`。

## AnnotationProcessor前言

在讲 AnnotationProcessor 之前，我们先想下，如何才能做到批量对 View 进行绑定，但是又不会消耗太多性能。

em...

最不消耗性能的方式，不就是直接使用 findViewById 去绑定 View 吗？既然如此，那么有没有什么方式能够做到在编译阶段就生成好 findViewById 这些代码，到时使用时，直接调用就行了。

这个想法貌似没有问题，那我们先看看生成好的 findViewById 的这些代码是长什么样的。

模拟生成好的代码样式：

```
public class MyButterKnife {
    public static void bind(MainActivity activity) {
        activity.tvContent = activity.findViewById(R.id.tv_content);
    }
}
```

不过，这样还是有问题，因为 bind() 方法里面应该传进去的是 Activity，这里固定写为 MainActivity 是不对的，我们应该要根据所传进来的 Activity 是什么，来决定加载哪个类的 findViewById 的代码，所以我们要创建新类，这个类名有固定的形式，那就是`原Activity名字+Binding`：

模拟自动生成的文件：

```
public class MainActivityBinding {
    public MainActivityBinding(MainActivity activity) {
        activity.tvContent = activity.findViewById(R.id.tv_content);
    }
}
```

修改后的 MyButterKnife

```
public class MyButterKnife {
    public static void bind(Activity activity) {
        try {
            //获取"当前的activity类名+Binding"的class对象
            Class bindingClass = Class.forName(activity.getClass().getCanonicalName() + "Binding");
            //获取class对象的构造方法，该构造方法的参数为当前的activity对象
            Constructor constructor = bindingClass.getDeclaredConstructor(activity.getClass());
            //调用构造方法
            constructor.newInstance(activity);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

运行！

我看了下手机，运行依旧是没问题的！

<img src="https://img-blog.csdnimg.cn/20210325200242185.jpg" width = "150" >

所以，我们现在只剩下一个问题了，那就是如何动态生成 MainActivityBinding 这个类了。

这时，就是真正需要用到 AnnotationProcessor。

AnnotationProcessor 是一种处理注解的工具，能够在源代码中查找出注解，并且根据注解自动生成代码。

## AnnotationProcessor使用

首先，我们要新建一个 module。

Android Studio --> File --> New Module --> Java or Kotlin Library --> Next -->

至于命名，大家依据自己的情况：

<img src="https://img-blog.csdnimg.cn/20210325212222573.jpg" width = "550" >

然后点击 Finish 按钮。

首先，让 MyBindingProcessor 继承 AbstractProcessor，

- `process()`：里面存放着自动生成代码的代码。
- `getSupportedAnnotationTypes()`：返回所支持注解类型。

```
public class MyBindingProcessor extends AbstractProcessor {
    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        return false;
    }
    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return super.getSupportedAnnotationTypes();
    }
}
```

不过，要使得 MyBindingProcessor 能够自动被调用，我们还需要稍微配置下：

<img src="https://img-blog.csdnimg.cn/20210325220447561.jpg" width = "650" >

### 项目结构更改

另外，为了功能解耦以及后续功能开发，我们需要把`MyButterKnife.java`和`MyBindView.java`文件单独抽取出来，各自使用一个 module 进行存放，目录结构如下：

<img src="https://img-blog.csdnimg.cn/20210325221444252.jpg" width = "250" >

app 依赖：

```
    implementation project(path: ':my-reflection')
    annotationProcessor project(':my-processor')
```

my-reflection 依赖：

```
    api project(path: ':my-annotations')
```

my-processor 依赖：

```
    implementation project(':my-annotations')
```

好了，一切准备就绪。

<img src="https://img-blog.csdnimg.cn/2021032522191218.jpg" width = "150" >

我们看回`MyBindingProcessor.class`，增加了注解支持和打印了日志。毕竟我们要测试下配置有没有什么问题，有的话，返回去看看哪里错漏，没有再继续写下去。

```
public class MyBindingProcessor extends AbstractProcessor {
    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        //测试输出
        System.out.println("配置成功！");
        return false;
    }
    @Override
    public Set<String> getSupportedAnnotationTypes() {
        //只支持 MyBindView 注解
        return Collections.singleton(MyBindView.class.getCanonicalName());
    }
}
```

### 测试配置
打开 Terminal，输入`./gradlew :app:compileDebugJava  `，查看输出有没有`配置成功！`，有的话，就是配置成功了！！！

<img src="https://img-blog.csdnimg.cn/20210325222932191.jpg" width = "450" >

### 代码生成

由于用了新的库，要多加一个依赖：

```
    implementation 'com.squareup:javapoet:1.12.1'
```

大量代码涌入，警报！警报！！

不过也不用担心，我基本都注释过了，很容易能看懂，即使看不懂也没关系，主要是知道整套流程和逻辑，到时需要真正去做的时候，再慢慢研究，毕竟做个 Demo 和做个上线项目，是两回事。

```
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
```

运行看了下，还是没有问题的，我们看看 build 目录有没有文件生成：

<img src="https://img-blog.csdnimg.cn/20210326111253754.jpg" width = "650" >

<p>

<img src="https://img-blog.csdnimg.cn/20210326111339727.jpg" width = "150" >

完结撒花，✿✿ヽ(°▽°)ノ✿

em...突然记起一件事，因为自动生成代码时在编译时间段执行的，所以注解在编译后，也就是字节码阶段是不需要存在的，所以，将其改为保留在源码阶段即可。

```
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.SOURCE)
public @interface MyBindView {
    int value();
}
```

源码地址：


<a href="https://github.com/bjsdm/MyButterKnife">https://github.com/bjsdm/MyButterKnife</a>

---

这是我的公众号，关注获取第一信息！！欢迎关注支持下，谢谢！

<img src="https://img-blog.csdnimg.cn/20210318100217713.png" width = "500" >
























































