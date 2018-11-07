# Spring Boot

## SpringApplication

    @SpringBootApplication
    public class MainApp {
    
        public static void main(String[] args) {
            SpringApplication.run(MainApp.class, args);
        }
    }

SpringApplication.run(MainApp.class, args);
静态方法run会在其内部构造一个SpringApplication实例并调用该实例的run方法完成Spring Boot项目的启动。
也可以使用SpringApplicationBuilder或者使用SpringApplication的构造方法来更灵活的应用Spring Boot，甚至可以扩展SpringApplication。

### 构造SpringApplication的实例

	public SpringApplication(ResourceLoader resourceLoader, Class<?>... primarySources) {
		this.resourceLoader = resourceLoader;
		Assert.notNull(primarySources, "PrimarySources must not be null");
		//
		this.primarySources = new LinkedHashSet<>(Arrays.asList(primarySources));
		//确定web app的类型
		this.webApplicationType = WebApplicationType.deduceFromClasspath();
		//从spring.factories加载并实例化所有ApplicationContextInitializer的实现类
		setInitializers((Collection) getSpringFactoriesInstances(
				ApplicationContextInitializer.class));
		//从spring.factories加载并实例化所有ApplicationListener的实现类
		setListeners((Collection) getSpringFactoriesInstances(ApplicationListener.class));
		this.mainApplicationClass = deduceMainApplicationClass();
	}

ApplicationContextInitializer

	# Application Context Initializers
	org.springframework.context.ApplicationContextInitializer=\
	org.springframework.boot.context.ConfigurationWarningsApplicationContextInitializer,\
	org.springframework.boot.context.ContextIdApplicationContextInitializer,\
	org.springframework.boot.context.config.DelegatingApplicationContextInitializer,\
	org.springframework.boot.web.context.ServerPortInfoApplicationContextInitializer

	# Initializers
	org.springframework.context.ApplicationContextInitializer=\
	org.springframework.boot.autoconfigure.SharedMetadataReaderFactoryContextInitializer,\
	org.springframework.boot.autoconfigure.logging.ConditionEvaluationReportLoggingListener

用户初始化ApplicationContext的回调方法，在ApplicationContext创建之后、refresh方法被调用之前，由SpringApplication执行其initialize逻辑，此时可以对ApplicationContext做一些自定义的处理。其实就是在prepareContext时。

ApplicationListener

	# Application Listeners
	org.springframework.context.ApplicationListener=\
	org.springframework.boot.ClearCachesApplicationListener,\
	org.springframework.boot.builder.ParentContextCloserApplicationListener,\
	org.springframework.boot.context.FileEncodingApplicationListener,\
	org.springframework.boot.context.config.AnsiOutputApplicationListener,\
	org.springframework.boot.context.config.ConfigFileApplicationListener,\
	org.springframework.boot.context.config.DelegatingApplicationListener,\
	org.springframework.boot.context.logging.ClasspathLoggingApplicationListener,\
	org.springframework.boot.context.logging.LoggingApplicationListener,\
	org.springframework.boot.liquibase.LiquibaseServiceLocatorApplicationListener

	# Application Listeners
	org.springframework.context.ApplicationListener=\
	org.springframework.boot.autoconfigure.BackgroundPreinitializer

观察者，观察ApplicationEvent事件并作出响应，如ConfigFileApplicationListener监听ApplicationEnvironmentPreparedEvent、ApplicationPreparedEvent。
当ApplicationEnvironmentPreparedEvent事件发送时，其从spring.factories中加载并实例化EnvironmentPostProcessor的实例，然后调用postProcessEnvironment方法读取配置文件处理environment。
当ApplicationPreparedEvent事件发送时，其向ApplicationContext添加一个PropertySourceOrderingPostProcessor实例，在context的初始化过程中对property资源重排序。


**可以自己实现ApplicationContextInitializer、ApplicationListener并将其配置到spring.factories文件中来实现对Spring Boot应用的定制。**

### SpringApplication.run(String... args)

	public ConfigurableApplicationContext.run(String... args) {
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();
		ConfigurableApplicationContext context = null;
		Collection<SpringBootExceptionReporter> exceptionReporters = new ArrayList<>();
		configureHeadlessProperty();
		//监听SpringApplication的run方法。
		//从spring.factories加载并实例化所有SpringApplicationRunListener的实现类
		SpringApplicationRunListeners listeners = getRunListeners(args);
		//ApplicationStartingEvent
		listeners.starting();
		try {
			ApplicationArguments applicationArguments = new DefaultApplicationArguments(
					args);
			//创建environment并调用EnvironmentPostProcessor#postProcessEnvironment进行配置文件读取
			//ApplicationEnvironmentPreparedEvent
			ConfigurableEnvironment environment = prepareEnvironment(listeners,
					applicationArguments);
			configureIgnoreBeanInfo(environment);
			Banner printedBanner = printBanner(environment);
			//根据this.webApplicationType加载并实例化对应的ApplicationContext实现类
			context = createApplicationContext();
			exceptionReporters = getSpringFactoriesInstances(
					SpringBootExceptionReporter.class,
					new Class[] { ConfigurableApplicationContext.class }, context);
			//调用ApplicationContextInitializer#initialize
			//将MainApp注册到BeanFactory
			//ApplicationPreparedEvent
			prepareContext(context, environment, listeners, applicationArguments,
					printedBanner);
			//调用AbstractApplicationContext#refresh	
			//ContextRefreshedEvent
			refreshContext(context);
			//空方法，子类可以充重写来扩展功能
			afterRefresh(context, applicationArguments);
			stopWatch.stop();
			if (this.logStartupInfo) {
				new StartupInfoLogger(this.mainApplicationClass)
						.logStarted(getApplicationLog(), stopWatch);
			}
			//ApplicationStartedEvent
			listeners.started(context);
			//调用ApplicationRunner、CommandLineRunner
			callRunners(context, applicationArguments);
		}
		catch (Throwable ex) {
			handleRunFailure(context, ex, exceptionReporters, listeners);
			throw new IllegalStateException(ex);
		}

		try {
			//ApplicationReadyEvent
			listeners.running(context);
		}
		catch (Throwable ex) {
			handleRunFailure(context, ex, exceptionReporters, null);
			throw new IllegalStateException(ex);
		}
		return context;
	}

#### SpringApplicationRunListeners listeners = getRunListeners(args);

从spring.factories中加载并实例化接口EventPublishingRunListener的实现类。
    
    # Run Listeners
    org.springframework.boot.SpringApplicationRunListener=\
    org.springframework.boot.context.event.EventPublishingRunListener
    
目前为止，接口SpringApplicationRunListener的实现类只有一个，就是EventPublishingRunListener。

**可以自己实现SpringApplicationRunListener并将其配置到spring.factories中来实现对SpringApplication的run方法的监听。**

	public EventPublishingRunListener(SpringApplication application, String[] args) {
		this.application = application;
		this.args = args;
		this.initialMulticaster = new SimpleApplicationEventMulticaster();
		for (ApplicationListener<?> listener : application.getListeners()) {
			this.initialMulticaster.addApplicationListener(listener);
		}
	}

将SpringApplication实例的所有ApplicationListener添加到EventPublishingRunListener实例的initialMulticaster属性中。
事件都将通过SpringApplicationRunListeners实例来发布。
   * ApplicationStartingEvent、ApplicationEnvironmentPreparedEvent、ApplicationEnvironmentPreparedEvent、ApplicationPreparedEvent事件会借助EventPublishingRunListener来发布。
   * ApplicationStartedEvent、ApplicationReadyEvent事件却是使用ApplicationContext发布。


#### ConfigurableEnvironment environment = prepareEnvironment(listeners, applicationArguments);

创建并初始化ConfigurableEnvironment实例。
创建ConfigurableEnvironment实例完成后会发布一个ApplicationEnvironmentPreparedEvent事件来调用相应的ApplicationListener对Environment做进一步的处理。
其中有一个ConfigFileApplicationListener，以这个类为例。

	@Override
	public void onApplicationEvent(ApplicationEvent event) {
		if (event instanceof ApplicationEnvironmentPreparedEvent) {
			onApplicationEnvironmentPreparedEvent(
					(ApplicationEnvironmentPreparedEvent) event);
		}
		if (event instanceof ApplicationPreparedEvent) {
			onApplicationPreparedEvent(event);
		}
	}
	
看看处理ApplicationEnvironmentPreparedEvent事件的逻辑。

	private void onApplicationEnvironmentPreparedEvent(
			ApplicationEnvironmentPreparedEvent event) {
		//从spring.factories文件中加载并实例化EnvironmentPostProcessor的实现类
		//postProcessors = {ArrayList@2074}  size = 3
        //0 = {SystemEnvironmentPropertySourceEnvironmentPostProcessor@2078} 
        //1 = {SpringApplicationJsonEnvironmentPostProcessor@2079} 
        //2 = {CloudFoundryVcapEnvironmentPostProcessor@2080} 
		List<EnvironmentPostProcessor> postProcessors = loadPostProcessors();
		//ConfigFileApplicationListener也是一个EnvironmentPostProcessor的实现类
		postProcessors.add(this);
		//排序
		AnnotationAwareOrderComparator.sort(postProcessors);
		//调用EnvironmentPostProcessor的postProcessEnvironment方法
		for (EnvironmentPostProcessor postProcessor : postProcessors) {
			postProcessor.postProcessEnvironment(event.getEnvironment(),
					event.getSpringApplication());
		}
	}

具体怎样加载配置文件就在这个ConfigFileApplicationListener的postProcessEnvironment方法了。
最终加载使用的是从spring.factories文件加载的PropertySourceLoader的两个实例进行的，分别用于处理properties文件和yml文件。

    # PropertySource Loaders
    org.springframework.boot.env.PropertySourceLoader=\
    org.springframework.boot.env.PropertiesPropertySourceLoader,\
    org.springframework.boot.env.YamlPropertySourceLoader

#### context = createApplicationContext();

	public AnnotationConfigApplicationContext() {
		this.reader = new AnnotatedBeanDefinitionReader(this);
		this.scanner = new ClassPathBeanDefinitionScanner(this);
	}

AnnotationConfigApplicationContext的父类持有一个DefaultListableBeanFactory实例。
这两个类实现了几个相同的接口：
    BeanDefinitionRegistry、HierarchicalBeanFactory、ListableBeanFactory、BeanFactory

	public GenericApplicationContext() {
		this.beanFactory = new DefaultListableBeanFactory();
	}

#### this.reader = new AnnotatedBeanDefinitionReader(this);

	AnnotationConfigUtils.registerAnnotationConfigProcessors(this.registry);

将
	org.springframework.context.annotation.internalConfigurationAnnotationProcessor：ConfigurationClassPostProcessor 处理@Configuration、@Bean、@Service、@Component
	org.springframework.context.annotation.internalAutowiredAnnotationProcessor：AutowiredAnnotationBeanPostProcessor 处理@Autowired、@Value，支持@Inject
	org.springframework.context.annotation.internalRequiredAnnotationProcessor：RequiredAnnotationBeanPostProcessor 处理@Required
	org.springframework.context.annotation.internalCommonAnnotationProcessor：CommonAnnotationBeanPostProcessor 处理@PostConstruct、@PreDestroy、@Resource、、、
	org.springframework.context.event.internalEventListenerProcessor：EventListenerMethodProcessor 处理@EventListener
	org.springframework.context.event.internalEventListenerFactory：DefaultEventListenerFactory 处理@EventListener

注册到BeanFactory

其中，ConfigurationClassPostProcessor是BeanFactoryPostProcessor的实现，同时也实现了BeanDefinitionRegistryPostProcessor，用于bean的注册；
AutowiredAnnotationBeanPostProcessor、RequiredAnnotationBeanPostProcessor、CommonAnnotationBeanPostProcessor是BeanPostProcessor的实现。
后面refresh时会用到这几个类。


#### prepareContext(context, environment, listeners, applicationArguments, printedBanner);

	private void prepareContext(ConfigurableApplicationContext context,
			ConfigurableEnvironment environment, SpringApplicationRunListeners listeners,
			ApplicationArguments applicationArguments, Banner printedBanner) {
		context.setEnvironment(environment);
		postProcessApplicationContext(context);
		//调用ApplicationContextInitializer#initialize
		applyInitializers(context);

		listeners.contextPrepared(context);
		if (this.logStartupInfo) {
			logStartupInfo(context.getParent() == null);
			logStartupProfileInfo(context);
		}

		// Add boot specific singleton beans
		// 注册applicationArguments到BeanFactory
		context.getBeanFactory().registerSingleton("springApplicationArguments",
				applicationArguments);
		if (printedBanner != null) {
			context.getBeanFactory().registerSingleton("springBootBanner", printedBanner);
		}

		// Load the sources
		Set<Object> sources = getAllSources();
		Assert.notEmpty(sources, "Sources must not be empty");
		//将MainApp注册到BeanFactory
		//使用org.springframework.boot.BeanDefinitionLoader将MainApp实例添加到BeanFactory
		load(context, sources.toArray(new Object[0]));
        //将SpringApplication中的ApplicationListener实例添加到ApplicationContext中		
		//ApplicationPreparedEvent
		listeners.contextLoaded(context);
	}


#### refreshContext(context);

调用了((AbstractApplicationContext) applicationContext).refresh();

## AbstractApplicationContext

### ((AbstractApplicationContext) applicationContext).refresh();

	public void refresh() throws BeansException, IllegalStateException {
		synchronized (this.startupShutdownMonitor) {
			// Prepare this context for refreshing.
			prepareRefresh();

			// Tell the subclass to refresh the internal bean factory.
			ConfigurableListableBeanFactory beanFactory = obtainFreshBeanFactory();

			// Prepare the bean factory for use in this context.
			prepareBeanFactory(beanFactory);

			try {
				// Allows post-processing of the bean factory in context subclasses.
				// 空方法，子类可以复写该方法对ApplicationContext进行扩展
				postProcessBeanFactory(beanFactory);

				// Invoke factory processors registered as beans in the context.
				// 组件扫描、注册
				invokeBeanFactoryPostProcessors(beanFactory);

				// Register bean processors that intercept bean creation.
				// 注册BeanPostProcessor
				registerBeanPostProcessors(beanFactory);

				// Initialize message source for this context.
				// 国际化
				initMessageSource();

				// Initialize event multicaster for this context.
				// 初始化applicationEventMulticaster
				initApplicationEventMulticaster();

				// Initialize other special beans in specific context subclasses.
				onRefresh();

				// Check for listener beans and register them.
				// 注册Listener
				registerListeners();

				// Instantiate all remaining (non-lazy-init) singletons.
				// 没有被@Lazy注解的bean的创建
				finishBeanFactoryInitialization(beanFactory);

				// Last step: publish corresponding event.
				// ContextRefreshedEvent
				finishRefresh();
			}

			catch (BeansException ex) {
				if (logger.isWarnEnabled()) {
					logger.warn("Exception encountered during context initialization - " +
							"cancelling refresh attempt: " + ex);
				}

				// Destroy already created singletons to avoid dangling resources.
				destroyBeans();

				// Reset 'active' flag.
				cancelRefresh(ex);

				// Propagate exception to caller.
				throw ex;
			}

			finally {
				// Reset common introspection caches in Spring's core, since we
				// might not ever need metadata for singleton beans anymore...
				resetCommonCaches();
			}
		}
	}

### invokeBeanFactoryPostProcessors(beanFactory);


从BeanFactory获取BeanDefinitionRegistryPostProcessor的实例。

ConfigurationClassPostProcessor：postProcessBeanDefinitionRegistry方法完成bean的扫描、注册

PostProcessorRegistrationDelegate.invokeBeanFactoryPostProcessors(beanFactory, getBeanFactoryPostProcessors());

ConfigurationClassParser：处理@Configuration的类

处理@PropertySources、@PropertySource
处理@ComponentScans、@ComponentScan
	使用ComponentScanAnnotationParser
		使用ClassPathBeanDefinitionScanner
@Import
处理@ImportResource
处理@Bean

ConfigurationClassBeanDefinitionReader：处理@Configuration类本身及从ConfigurationClassParser得到的@Bean方法

### finishBeanFactoryInitialization(beanFactory);

完成非@Lazy的创建

调用beanFactory.preInstantiateSingletons(); --->> getBean --->> doGetBean

    protected <T> T doGetBean(final String name, @Nullable final Class<T> requiredType,
			@Nullable final Object[] args, boolean typeCheckOnly) throws BeansException {

		final String beanName = transformedBeanName(name);
		Object bean;

		// Eagerly check singleton cache for manually registered singletons.
		//尝试获取以创建的bean对象，或者使用ObjectFactory创建bean对象
		Object sharedInstance = getSingleton(beanName);
		if (sharedInstance != null && args == null) {
			if (logger.isDebugEnabled()) {
				if (isSingletonCurrentlyInCreation(beanName)) {
					logger.debug("Returning eagerly cached instance of singleton bean '" + beanName +
							"' that is not fully initialized yet - a consequence of a circular reference");
				}
				else {
					logger.debug("Returning cached instance of singleton bean '" + beanName + "'");
				}
			}
			bean = getObjectForBeanInstance(sharedInstance, name, beanName, null);
		}

		else {
			// Fail if we're already creating this bean instance:
			// We're assumably within a circular reference.
			if (isPrototypeCurrentlyInCreation(beanName)) {
				throw new BeanCurrentlyInCreationException(beanName);
			}

			// Check if bean definition exists in this factory.
			BeanFactory parentBeanFactory = getParentBeanFactory();
			if (parentBeanFactory != null && !containsBeanDefinition(beanName)) {
				// Not found -> check parent.
				String nameToLookup = originalBeanName(name);
				if (parentBeanFactory instanceof AbstractBeanFactory) {
					return ((AbstractBeanFactory) parentBeanFactory).doGetBean(
							nameToLookup, requiredType, args, typeCheckOnly);
				}
				else if (args != null) {
					// Delegation to parent with explicit args.
					return (T) parentBeanFactory.getBean(nameToLookup, args);
				}
				else {
					// No args -> delegate to standard getBean method.
					return parentBeanFactory.getBean(nameToLookup, requiredType);
				}
			}

			if (!typeCheckOnly) {
				markBeanAsCreated(beanName);
			}

			try {
				final RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);
				checkMergedBeanDefinition(mbd, beanName, args);

				// Guarantee initialization of beans that the current bean depends on.
				// 这里为空？？？
				String[] dependsOn = mbd.getDependsOn();
				if (dependsOn != null) {
					for (String dep : dependsOn) {
						if (isDependent(beanName, dep)) {
							throw new BeanCreationException(mbd.getResourceDescription(), beanName,
									"Circular depends-on relationship between '" + beanName + "' and '" + dep + "'");
						}
						registerDependentBean(dep, beanName);
						try {
							getBean(dep);
						}
						catch (NoSuchBeanDefinitionException ex) {
							throw new BeanCreationException(mbd.getResourceDescription(), beanName,
									"'" + beanName + "' depends on missing bean '" + dep + "'", ex);
						}
					}
				}

				// Create bean instance.
				if (mbd.isSingleton()) {
				    //创建单例bean
					sharedInstance = getSingleton(beanName, () -> {
						try {
							return createBean(beanName, mbd, args);
						}
						catch (BeansException ex) {
							// Explicitly remove instance from singleton cache: It might have been put there
							// eagerly by the creation process, to allow for circular reference resolution.
							// Also remove any beans that received a temporary reference to the bean.
							destroySingleton(beanName);
							throw ex;
						}
					});
					bean = getObjectForBeanInstance(sharedInstance, name, beanName, mbd);
				}

				else if (mbd.isPrototype()) {
					// It's a prototype -> create a new instance.
					Object prototypeInstance = null;
					try {
						beforePrototypeCreation(beanName);
						prototypeInstance = createBean(beanName, mbd, args);
					}
					finally {
						afterPrototypeCreation(beanName);
					}
					bean = getObjectForBeanInstance(prototypeInstance, name, beanName, mbd);
				}

				else {
					String scopeName = mbd.getScope();
					final Scope scope = this.scopes.get(scopeName);
					if (scope == null) {
						throw new IllegalStateException("No Scope registered for scope name '" + scopeName + "'");
					}
					try {
						Object scopedInstance = scope.get(beanName, () -> {
							beforePrototypeCreation(beanName);
							try {
								return createBean(beanName, mbd, args);
							}
							finally {
								afterPrototypeCreation(beanName);
							}
						});
						bean = getObjectForBeanInstance(scopedInstance, name, beanName, mbd);
					}
					catch (IllegalStateException ex) {
						throw new BeanCreationException(beanName,
								"Scope '" + scopeName + "' is not active for the current thread; consider " +
								"defining a scoped proxy for this bean if you intend to refer to it from a singleton",
								ex);
					}
				}
			}
			catch (BeansException ex) {
				cleanupAfterBeanCreationFailure(beanName);
				throw ex;
			}
		}

		// Check if required type matches the type of the actual bean instance.
		if (requiredType != null && !requiredType.isInstance(bean)) {
			try {
				T convertedBean = getTypeConverter().convertIfNecessary(bean, requiredType);
				if (convertedBean == null) {
					throw new BeanNotOfRequiredTypeException(name, requiredType, bean.getClass());
				}
				return convertedBean;
			}
			catch (TypeMismatchException ex) {
				if (logger.isDebugEnabled()) {
					logger.debug("Failed to convert bean '" + name + "' to required type '" +
							ClassUtils.getQualifiedName(requiredType) + "'", ex);
				}
				throw new BeanNotOfRequiredTypeException(name, requiredType, bean.getClass());
			}
		}
		return (T) bean;
	}

#### Object sharedInstance = getSingleton(beanName); --->> getSingleton(beanName, true);
    

this.singletonObjects: 已创建的单例bean
this.singletonsCurrentlyInCreation：正在创建的beanName集合
this.earlySingletonObjects：由于循环依赖而被提前创建出来的单例bean
this.singletonFactories：用于创建bean的ObjectFactory


	protected Object getSingleton(String beanName, boolean allowEarlyReference) {
		Object singletonObject = this.singletonObjects.get(beanName);
		if (singletonObject == null && isSingletonCurrentlyInCreation(beanName)) {
			synchronized (this.singletonObjects) {
				singletonObject = this.earlySingletonObjects.get(beanName);
				if (singletonObject == null && allowEarlyReference) {
					ObjectFactory<?> singletonFactory = this.singletonFactories.get(beanName);
					if (singletonFactory != null) {
						singletonObject = singletonFactory.getObject();
						this.earlySingletonObjects.put(beanName, singletonObject);
						this.singletonFactories.remove(beanName);
					}
				}
			}
		}
		return singletonObject;
	}

#### 创建单例bean

    sharedInstance = getSingleton(beanName, () -> {
        try {
            return createBean(beanName, mbd, args);
        }
        catch (BeansException ex) {
            // Explicitly remove instance from singleton cache: It might have been put there
            // eagerly by the creation process, to allow for circular reference resolution.
            // Also remove any beans that received a temporary reference to the bean.
            destroySingleton(beanName);
            throw ex;
        }
    });
    
	public Object getSingleton(String beanName, ObjectFactory<?> singletonFactory) {
		Assert.notNull(beanName, "Bean name must not be null");
		synchronized (this.singletonObjects) {
			Object singletonObject = this.singletonObjects.get(beanName);
			if (singletonObject == null) {
				if (this.singletonsCurrentlyInDestruction) {
					throw new BeanCreationNotAllowedException(beanName,
							"Singleton bean creation not allowed while singletons of this factory are in destruction " +
							"(Do not request a bean from a BeanFactory in a destroy method implementation!)");
				}
				if (logger.isDebugEnabled()) {
					logger.debug("Creating shared instance of singleton bean '" + beanName + "'");
				}
				//记录当前beanName到singletonsCurrentlyInCreation，表示bean正在被创建
				beforeSingletonCreation(beanName);
				boolean newSingleton = false;
				boolean recordSuppressedExceptions = (this.suppressedExceptions == null);
				if (recordSuppressedExceptions) {
					this.suppressedExceptions = new LinkedHashSet<>();
				}
				try {
				    //创建bean，实际上是调用了createBean方法
					singletonObject = singletonFactory.getObject();
					newSingleton = true;
				}
				catch (IllegalStateException ex) {
					// Has the singleton object implicitly appeared in the meantime ->
					// if yes, proceed with it since the exception indicates that state.
					singletonObject = this.singletonObjects.get(beanName);
					if (singletonObject == null) {
						throw ex;
					}
				}
				catch (BeanCreationException ex) {
					if (recordSuppressedExceptions) {
						for (Exception suppressedException : this.suppressedExceptions) {
							ex.addRelatedCause(suppressedException);
						}
					}
					throw ex;
				}
				finally {
					if (recordSuppressedExceptions) {
						this.suppressedExceptions = null;
					}
					//从singletonsCurrentlyInCreation删除beanName，表示bean已创建
					afterSingletonCreation(beanName);
				}
				if (newSingleton) {
				    //this.singletonObjects.put(beanName, singletonObject);
                    //this.singletonFactories.remove(beanName);
                    //this.earlySingletonObjects.remove(beanName);
                    //this.registeredSingletons.add(beanName);
					addSingleton(beanName, singletonObject);
				}
			}
			return singletonObject;
		}
	}
    	
    	
	protected Object createBean(String beanName, RootBeanDefinition mbd, @Nullable Object[] args)
			throws BeanCreationException {

		if (logger.isDebugEnabled()) {
			logger.debug("Creating instance of bean '" + beanName + "'");
		}
		RootBeanDefinition mbdToUse = mbd;

		// Make sure bean class is actually resolved at this point, and
		// clone the bean definition in case of a dynamically resolved Class
		// which cannot be stored in the shared merged bean definition.
		Class<?> resolvedClass = resolveBeanClass(mbd, beanName);
		if (resolvedClass != null && !mbd.hasBeanClass() && mbd.getBeanClassName() != null) {
			mbdToUse = new RootBeanDefinition(mbd);
			mbdToUse.setBeanClass(resolvedClass);
		}

		// Prepare method overrides.
		try {
			mbdToUse.prepareMethodOverrides();
		}
		catch (BeanDefinitionValidationException ex) {
			throw new BeanDefinitionStoreException(mbdToUse.getResourceDescription(),
					beanName, "Validation of method overrides failed", ex);
		}

		try {
			// Give BeanPostProcessors a chance to return a proxy instead of the target bean instance.
			// 应用InstantiationAwareBeanPostProcessor.postProcessBeforeInstantiation
			// 调用BeanPostProcessor.postProcessAfterInitialization
			Object bean = resolveBeforeInstantiation(beanName, mbdToUse);
			if (bean != null) {
				return bean;
			}
		}
		catch (Throwable ex) {
			throw new BeanCreationException(mbdToUse.getResourceDescription(), beanName,
					"BeanPostProcessor before instantiation of bean failed", ex);
		}

		try {
			Object beanInstance = doCreateBean(beanName, mbdToUse, args);
			if (logger.isDebugEnabled()) {
				logger.debug("Finished creating instance of bean '" + beanName + "'");
			}
			return beanInstance;
		}
		catch (BeanCreationException | ImplicitlyAppearedSingletonException ex) {
			// A previously detected exception with proper bean creation context already,
			// or illegal singleton state to be communicated up to DefaultSingletonBeanRegistry.
			throw ex;
		}
		catch (Throwable ex) {
			throw new BeanCreationException(
					mbdToUse.getResourceDescription(), beanName, "Unexpected exception during bean creation", ex);
		}
	}
	
	protected Object doCreateBean(final String beanName, final RootBeanDefinition mbd, final @Nullable Object[] args)
			throws BeanCreationException {

		// Instantiate the bean.
		BeanWrapper instanceWrapper = null;
		if (mbd.isSingleton()) {
			instanceWrapper = this.factoryBeanInstanceCache.remove(beanName);
		}
		if (instanceWrapper == null) {
		    //使用factory-method、构造器等创建bean实例
			instanceWrapper = createBeanInstance(beanName, mbd, args);
		}
		final Object bean = instanceWrapper.getWrappedInstance();
		Class<?> beanType = instanceWrapper.getWrappedClass();
		if (beanType != NullBean.class) {
			mbd.resolvedTargetType = beanType;
		}

		// Allow post-processors to modify the merged bean definition.
		synchronized (mbd.postProcessingLock) {
			if (!mbd.postProcessed) {
				try {
					applyMergedBeanDefinitionPostProcessors(mbd, beanType, beanName);
				}
				catch (Throwable ex) {
					throw new BeanCreationException(mbd.getResourceDescription(), beanName,
							"Post-processing of merged bean definition failed", ex);
				}
				mbd.postProcessed = true;
			}
		}

		// Eagerly cache singletons to be able to resolve circular references
		// even when triggered by lifecycle interfaces like BeanFactoryAware.
		boolean earlySingletonExposure = (mbd.isSingleton() && this.allowCircularReferences &&
				isSingletonCurrentlyInCreation(beanName));
		if (earlySingletonExposure) {
			if (logger.isDebugEnabled()) {
				logger.debug("Eagerly caching bean '" + beanName +
						"' to allow for resolving potential circular references");
			}
			//this.singletonFactories.put(beanName, singletonFactory);
            //this.earlySingletonObjects.remove(beanName);
            //this.registeredSingletons.add(beanName);
            //getEarlyBeanReference(beanName, mbd, bean) aop织入
			addSingletonFactory(beanName, () -> getEarlyBeanReference(beanName, mbd, bean));
		}

		// Initialize the bean instance.
		Object exposedObject = bean;
		try {
		    //填充bean、属性赋值，如果依赖其它bean，递归调用getBean方法
			populateBean(beanName, mbd, instanceWrapper);
			//处理Aware
			//调用BeanPostProcessor.postProcessBeforeInitialization
			//处理InitializingBean
			//处理init-method
			//调用BeanPostProcessor.postProcessAfterInitialization
			exposedObject = initializeBean(beanName, exposedObject, mbd);
		}
		catch (Throwable ex) {
			if (ex instanceof BeanCreationException && beanName.equals(((BeanCreationException) ex).getBeanName())) {
				throw (BeanCreationException) ex;
			}
			else {
				throw new BeanCreationException(
						mbd.getResourceDescription(), beanName, "Initialization of bean failed", ex);
			}
		}

		if (earlySingletonExposure) {
			Object earlySingletonReference = getSingleton(beanName, false);
			if (earlySingletonReference != null) {
				if (exposedObject == bean) {
					exposedObject = earlySingletonReference;
				}
				else if (!this.allowRawInjectionDespiteWrapping && hasDependentBean(beanName)) {
					String[] dependentBeans = getDependentBeans(beanName);
					Set<String> actualDependentBeans = new LinkedHashSet<>(dependentBeans.length);
					for (String dependentBean : dependentBeans) {
						if (!removeSingletonIfCreatedForTypeCheckOnly(dependentBean)) {
							actualDependentBeans.add(dependentBean);
						}
					}
					if (!actualDependentBeans.isEmpty()) {
						throw new BeanCurrentlyInCreationException(beanName,
								"Bean with name '" + beanName + "' has been injected into other beans [" +
								StringUtils.collectionToCommaDelimitedString(actualDependentBeans) +
								"] in its raw version as part of a circular reference, but has eventually been " +
								"wrapped. This means that said other beans do not use the final version of the " +
								"bean. This is often the result of over-eager type matching - consider using " +
								"'getBeanNamesOfType' with the 'allowEagerInit' flag turned off, for example.");
					}
				}
			}
		}

		// Register bean as disposable.
		try {
			registerDisposableBeanIfNecessary(beanName, bean, mbd);
		}
		catch (BeanDefinitionValidationException ex) {
			throw new BeanCreationException(
					mbd.getResourceDescription(), beanName, "Invalid destruction signature", ex);
		}

		return exposedObject;
	}

### 循环依赖

以两个互相依赖的单例bean ServiceA和ServiceB为例。

    get a
        this.singletonsCurrentlyInCreation.add(a)
        this.singletonFactories.put(a, singletonFactory);
        this.earlySingletonObjects.remove(a);
        this.registeredSingletons.add(a);
            处理依赖 
                get b
                    this.singletonsCurrentlyInCreation.add(b)
                    this.singletonFactories.put(b, singletonFactory);
                    this.earlySingletonObjects.remove(b);
                    this.registeredSingletons.add(b);
                    处理依赖 
                        get a
                            a = this.singletonFactories.get(beanName).get();
                            this.earlySingletonObjects.put(a, singletonObject);
                            this.singletonFactories.remove(a);
                        <<---
                    this.singletonsCurrentlyInCreation.remove(b)
                    this.singletonObjects.put(b, singletonObject);
                    this.singletonFactories.remove(b);
                    this.earlySingletonObjects.remove(b);
                    this.registeredSingletons.add(b);  
                <<---    
        this.singletonsCurrentlyInCreation.remove(a)
        this.singletonObjects.put(a, singletonObject);
        this.singletonFactories.remove(a);
        this.earlySingletonObjects.remove(a);
        this.registeredSingletons.add(a);    

