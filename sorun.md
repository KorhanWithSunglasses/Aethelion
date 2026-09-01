Run chmod +x gradlew
Downloading https://services.gradle.org/distributions/gradle-8.9-bin.zip
.................................................................................................................................
Unzipping /home/runner/.gradle/wrapper/dists/gradle-8.9-bin/90cnw93cvbtalezasaz0blq0a/gradle-8.9-bin.zip to /home/runner/.gradle/wrapper/dists/gradle-8.9-bin/90cnw93cvbtalezasaz0blq0a
Set executable permissions for: /home/runner/.gradle/wrapper/dists/gradle-8.9-bin/90cnw93cvbtalezasaz0blq0a/gradle-8.9/bin/gradle

Welcome to Gradle 8.9!

Here are the highlights of this release:
 - Enhanced Error and Warning Messages
 - IDE Integration Improvements
 - Daemon JVM Information

For more details see https://docs.gradle.org/8.9/release-notes.html

Starting a Gradle Daemon (subsequent builds will be faster)

> Configure project :AsianAnimeProviders
Fetching JAR: cloudstream.jar

> Task :LiveTvProviders:checkKotlinGradlePluginConfigurationErrors SKIPPED
> Task :TurkishProviders:checkKotlinGradlePluginConfigurationErrors SKIPPED
> Task :AsianAnimeProviders:checkKotlinGradlePluginConfigurationErrors SKIPPED
> Task :GlobalProviders:checkKotlinGradlePluginConfigurationErrors SKIPPED
> Task :GlobalProviders:preBuild UP-TO-DATE
> Task :LiveTvProviders:preBuild UP-TO-DATE
> Task :GlobalProviders:preDebugBuild UP-TO-DATE
> Task :AsianAnimeProviders:preBuild UP-TO-DATE
> Task :AsianAnimeProviders:preDebugBuild UP-TO-DATE
> Task :TurkishProviders:preBuild UP-TO-DATE
> Task :TurkishProviders:preDebugBuild UP-TO-DATE
> Task :LiveTvProviders:preDebugBuild UP-TO-DATE
> Task :LiveTvProviders:generateDebugResValues
> Task :GlobalProviders:generateDebugResValues
> Task :AsianAnimeProviders:generateDebugResValues
> Task :TurkishProviders:generateDebugResValues
> Task :AsianAnimeProviders:generateDebugResources
> Task :LiveTvProviders:generateDebugResources
> Task :TurkishProviders:generateDebugResources
> Task :GlobalProviders:generateDebugResources
> Task :LiveTvProviders:packageDebugResources
> Task :TurkishProviders:packageDebugResources
> Task :AsianAnimeProviders:packageDebugResources
> Task :GlobalProviders:packageDebugResources
> Task :TurkishProviders:parseDebugLocalResources
> Task :AsianAnimeProviders:parseDebugLocalResources
> Task :LiveTvProviders:parseDebugLocalResources
> Task :GlobalProviders:parseDebugLocalResources
> Task :GlobalProviders:generateDebugRFile
> Task :LiveTvProviders:generateDebugRFile
> Task :TurkishProviders:generateDebugRFile
> Task :AsianAnimeProviders:generateDebugRFile
e: file:///home/runner/work/Aethelion/Aethelion/TurkishProviders/src/main/kotlin/com/hexated/DiziPalProvider.kt:239:40 Unresolved reference 'ExtractorLinkType'.

e: file:///home/runner/work/Aethelion/Aethelion/TurkishProviders/src/main/kotlin/com/hexated/FilmMakinesiProvider.kt:84:93 'fun String?.toRatingInt(): Int?' is deprecated. toRatingInt() is deprecated. Use new score API instead.
> Task :TurkishProviders:compileDebugKotlin
w: Argument -Xopt-in is deprecated. Please use -opt-in instead
e: file:///home/runner/work/Aethelion/Aethelion/TurkishProviders/src/main/kotlin/com/hexated/FilmMakinesiProvider.kt:106:18 'var rating: Int?' is deprecated. `rating` is the old scoring system, use score instead.
e: file:///home/runner/work/Aethelion/Aethelion/TurkishProviders/src/main/kotlin/com/hexated/FullHDFilmizleseneProvider.kt:65:90 'fun String?.toRatingInt(): Int?' is deprecated. toRatingInt() is deprecated. Use new score API instead.
e: file:///home/runner/work/Aethelion/Aethelion/TurkishProviders/src/main/kotlin/com/hexated/FullHDFilmizleseneProvider.kt:74:18 'var rating: Int?' is deprecated. `rating` is the old scoring system, use score instead.
e: file:///home/runner/work/Aethelion/Aethelion/TurkishProviders/src/main/kotlin/com/hexated/HDFilmCehennemiProvider.kt:102:116 'fun String?.toRatingInt(): Int?' is deprecated. toRatingInt() is deprecated. Use new score API instead.
e: file:///home/runner/work/Aethelion/Aethelion/TurkishProviders/src/main/kotlin/com/hexated/HDFilmCehennemiProvider.kt:142:22 'var rating: Int?' is deprecated. `rating` is the old scoring system, use score instead.
e: file:///home/runner/work/Aethelion/Aethelion/TurkishProviders/src/main/kotlin/com/hexated/HDFilmCehennemiProvider.kt:153:22 'var rating: Int?' is deprecated. `rating` is the old scoring system, use score instead.
e: file:///home/runner/work/Aethelion/Aethelion/TurkishProviders/src/main/kotlin/com/hexated/HDFilmCehennemiProvider.kt:178:21 'constructor(source: String, name: String, url: String, referer: String, quality: Int, type: ExtractorLinkType?, headers: Map<String, String> = ..., extractorData: String? = ...): ExtractorLink' is deprecated. Use newExtractorLink.
e: file:///home/runner/work/Aethelion/Aethelion/TurkishProviders/src/main/kotlin/com/hexated/JetFilmIzleProvider.kt:79:118 'fun String?.toRatingInt(): Int?' is deprecated. toRatingInt() is deprecated. Use new score API instead.
e: file:///home/runner/work/Aethelion/Aethelion/TurkishProviders/src/main/kotlin/com/hexated/JetFilmIzleProvider.kt:104:18 'var rating: Int?' is deprecated. `rating` is the old scoring system, use score instead.
e: file:///home/runner/work/Aethelion/Aethelion/TurkishProviders/src/main/kotlin/com/hexated/extractors/CloseLoad.kt:37:27 Unresolved reference. None of the following candidates is applicable because of a receiver type mismatch:
fun MainAPI.fixUrl(url: String): String
e: file:///home/runner/work/Aethelion/Aethelion/TurkishProviders/src/main/kotlin/com/hexated/extractors/CloseLoad.kt:53:29 'constructor(source: String, name: String, url: String, referer: String, quality: Int, isM3u8: Boolean = ..., headers: Map<String, String> = ..., extractorData: String? = ...): ExtractorLink' is deprecated. Use newExtractorLink.
e: file:///home/runner/work/Aethelion/Aethelion/TurkishProviders/src/main/kotlin/com/hexated/extractors/PixelDrain.kt:24:13 'constructor(source: String, name: String, url: String, referer: String, quality: Int, type: ExtractorLinkType?, headers: Map<String, String> = ..., extractorData: String? = ...): ExtractorLink' is deprecated. Use newExtractorLink.
e: file:///home/runner/work/Aethelion/Aethelion/TurkishProviders/src/main/kotlin/com/hexated/extractors/RapidVid.kt:33:31 Unresolved reference. None of the following candidates is applicable because of a receiver type mismatch:
fun MainAPI.fixUrl(url: String): String
e: file:///home/runner/work/Aethelion/Aethelion/TurkishProviders/src/main/kotlin/com/hexated/extractors/RapidVid.kt:55:17 'constructor(source: String, name: String, url: String, referer: String, quality: Int, isM3u8: Boolean = ..., headers: Map<String, String> = ..., extractorData: String? = ...): ExtractorLink' is deprecated. Use newExtractorLink.
e: file:///home/runner/work/Aethelion/Aethelion/TurkishProviders/src/main/kotlin/com/hexated/extractors/Sobreatsesuyp.kt:44:17 'constructor(source: String, name: String, url: String, referer: String, quality: Int, type: ExtractorLinkType?, headers: Map<String, String> = ..., extractorData: String? = ...): ExtractorLink' is deprecated. Use newExtractorLink.
e: file:///home/runner/work/Aethelion/Aethelion/TurkishProviders/src/main/kotlin/com/hexated/extractors/TRsTX.kt:44:17 'constructor(source: String, name: String, url: String, referer: String, quality: Int, type: ExtractorLinkType?, headers: Map<String, String> = ..., extractorData: String? = ...): ExtractorLink' is deprecated. Use newExtractorLink.
e: file:///home/runner/work/Aethelion/Aethelion/TurkishProviders/src/main/kotlin/com/hexated/extractors/TurboImgz.kt:26:13 'constructor(source: String, name: String, url: String, referer: String, quality: Int, isM3u8: Boolean = ..., headers: Map<String, String> = ..., extractorData: String? = ...): ExtractorLink' is deprecated. Use newExtractorLink.
e: file:///home/runner/work/Aethelion/Aethelion/TurkishProviders/src/main/kotlin/com/hexated/extractors/VidMoxy.kt:33:31 Unresolved reference. None of the following candidates is applicable because of a receiver type mismatch:
fun MainAPI.fixUrl(url: String): String
e: file:///home/runner/work/Aethelion/Aethelion/TurkishProviders/src/main/kotlin/com/hexated/extractors/VidMoxy.kt:55:17 'constructor(source: String, name: String, url: String, referer: String, quality: Int, isM3u8: Boolean = ..., headers: Map<String, String> = ..., extractorData: String? = ...): ExtractorLink' is deprecated. Use newExtractorLink.

> Task :TurkishProviders:compileDebugKotlin FAILED

> Task :LiveTvProviders:compileDebugKotlin
w: Argument -Xopt-in is deprecated. Please use -opt-in instead
w: file:///home/runner/work/Aethelion/Aethelion/LiveTvProviders/src/main/kotlin/com/hexated/CanliYayinProvider.kt:109:13 'constructor(source: String, name: String, url: String, referer: String, quality: Int, headers: Map<String, String> = ..., extractorData: String? = ..., type: ExtractorLinkType, audioTracks: List<AudioFile> = ...): ExtractorLink' is deprecated. Use newExtractorLink.
w: file:///home/runner/work/Aethelion/Aethelion/LiveTvProviders/src/main/kotlin/com/hexated/UlusalTvProvider.kt:160:13 'constructor(source: String, name: String, url: String, referer: String, quality: Int, headers: Map<String, String> = ..., extractorData: String? = ..., type: ExtractorLinkType, audioTracks: List<AudioFile> = ...): ExtractorLink' is deprecated. Use newExtractorLink.
w: file:///home/runner/work/Aethelion/Aethelion/LiveTvProviders/src/main/kotlin/com/hexated/core/ExtractorHelper.kt:22:17 'constructor(source: String, name: String, url: String, referer: String, quality: Int, headers: Map<String, String> = ..., extractorData: String? = ..., type: ExtractorLinkType, audioTracks: List<AudioFile> = ...): ExtractorLink' is deprecated. Use newExtractorLink.
w: file:///home/runner/work/Aethelion/Aethelion/LiveTvProviders/src/main/kotlin/com/hexated/core/ExtractorHelper.kt:38:17 'constructor(source: String, name: String, url: String, referer: String, quality: Int, headers: Map<String, String> = ..., extractorData: String? = ..., type: ExtractorLinkType, audioTracks: List<AudioFile> = ...): ExtractorLink' is deprecated. Use newExtractorLink.
w: file:///home/runner/work/Aethelion/Aethelion/LiveTvProviders/src/main/kotlin/com/hexated/extractors/CloseLoad.kt:31:21 'constructor(source: String, name: String, url: String, referer: String, quality: Int, headers: Map<String, String> = ..., extractorData: String? = ..., type: ExtractorLinkType, audioTracks: List<AudioFile> = ...): ExtractorLink' is deprecated. Use newExtractorLink.
w: file:///home/runner/work/Aethelion/Aethelion/LiveTvProviders/src/main/kotlin/com/hexated/extractors/FileLions.kt:35:21 'constructor(source: String, name: String, url: String, referer: String, quality: Int, headers: Map<String, String> = ..., extractorData: String? = ..., type: ExtractorLinkType, audioTracks: List<AudioFile> = ...): ExtractorLink' is deprecated. Use newExtractorLink.
w: file:///home/runner/work/Aethelion/Aethelion/LiveTvProviders/src/main/kotlin/com/hexated/extractors/Rapidame.kt:35:21 'constructor(source: String, name: String, url: String, referer: String, quality: Int, headers: Map<String, String> = ..., extractorData: String? = ..., type: ExtractorLinkType, audioTracks: List<AudioFile> = ...): ExtractorLink' is deprecated. Use newExtractorLink.
w: file:///home/runner/work/Aethelion/Aethelion/LiveTvProviders/src/main/kotlin/com/hexated/extractors/Streamwish.kt:35:21 'constructor(source: String, name: String, url: String, referer: String, quality: Int, headers: Map<String, String> = ..., extractorData: String? = ..., type: ExtractorLinkType, audioTracks: List<AudioFile> = ...): ExtractorLink' is deprecated. Use newExtractorLink.
w: file:///home/runner/work/Aethelion/Aethelion/LiveTvProviders/src/main/kotlin/com/hexated/extractors/Vidmoly.kt:35:21 'constructor(source: String, name: String, url: String, referer: String, quality: Int, headers: Map<String, String> = ..., extractorData: String? = ..., type: ExtractorLinkType, audioTracks: List<AudioFile> = ...): ExtractorLink' is deprecated. Use newExtractorLink.

> Task :GlobalProviders:compileDebugKotlin
w: Argument -Xopt-in is deprecated. Please use -opt-in instead
w: file:///home/runner/work/Aethelion/Aethelion/GlobalProviders/src/main/kotlin/com/hexated/LookMovieProvider.kt:164:41 'constructor(lang: String, url: String): SubtitleFile' is deprecated. Use newSubtitleFile method.
w: file:///home/runner/work/Aethelion/Aethelion/GlobalProviders/src/main/kotlin/com/hexated/core/ExtractorHelper.kt:22:17 'constructor(source: String, name: String, url: String, referer: String, quality: Int, headers: Map<String, String> = ..., extractorData: String? = ..., type: ExtractorLinkType, audioTracks: List<AudioFile> = ...): ExtractorLink' is deprecated. Use newExtractorLink.
w: file:///home/runner/work/Aethelion/Aethelion/GlobalProviders/src/main/kotlin/com/hexated/core/ExtractorHelper.kt:38:17 'constructor(source: String, name: String, url: String, referer: String, quality: Int, headers: Map<String, String> = ..., extractorData: String? = ..., type: ExtractorLinkType, audioTracks: List<AudioFile> = ...): ExtractorLink' is deprecated. Use newExtractorLink.
w: file:///home/runner/work/Aethelion/Aethelion/GlobalProviders/src/main/kotlin/com/hexated/extractors/CloseLoad.kt:31:21 'constructor(source: String, name: String, url: String, referer: String, quality: Int, headers: Map<String, String> = ..., extractorData: String? = ..., type: ExtractorLinkType, audioTracks: List<AudioFile> = ...): ExtractorLink' is deprecated. Use newExtractorLink.
w: file:///home/runner/work/Aethelion/Aethelion/GlobalProviders/src/main/kotlin/com/hexated/extractors/FileLions.kt:35:21 'constructor(source: String, name: String, url: String, referer: String, quality: Int, headers: Map<String, String> = ..., extractorData: String? = ..., type: ExtractorLinkType, audioTracks: List<AudioFile> = ...): ExtractorLink' is deprecated. Use newExtractorLink.
w: file:///home/runner/work/Aethelion/Aethelion/GlobalProviders/src/main/kotlin/com/hexated/extractors/Rapidame.kt:35:21 'constructor(source: String, name: String, url: String, referer: String, quality: Int, headers: Map<String, String> = ..., extractorData: String? = ..., type: ExtractorLinkType, audioTracks: List<AudioFile> = ...): ExtractorLink' is deprecated. Use newExtractorLink.
w: file:///home/runner/work/Aethelion/Aethelion/GlobalProviders/src/main/kotlin/com/hexated/extractors/Streamwish.kt:35:21 'constructor(source: String, name: String, url: String, referer: String, quality: Int, headers: Map<String, String> = ..., extractorData: String? = ..., type: ExtractorLinkType, audioTracks: List<AudioFile> = ...): ExtractorLink' is deprecated. Use newExtractorLink.
w: file:///home/runner/work/Aethelion/Aethelion/GlobalProviders/src/main/kotlin/com/hexated/extractors/Vidmoly.kt:35:21 'constructor(source: String, name: String, url: String, referer: String, quality: Int, headers: Map<String, String> = ..., extractorData: String? = ..., type: ExtractorLinkType, audioTracks: List<AudioFile> = ...): ExtractorLink' is deprecated. Use newExtractorLink.

> Task :AsianAnimeProviders:compileDebugKotlin
w: Argument -Xopt-in is deprecated. Please use -opt-in instead
w: file:///home/runner/work/Aethelion/Aethelion/AsianAnimeProviders/src/main/kotlin/com/hexated/AnimecixProvider.kt:145:41 'constructor(lang: String, url: String): SubtitleFile' is deprecated. Use newSubtitleFile method.
w: file:///home/runner/work/Aethelion/Aethelion/AsianAnimeProviders/src/main/kotlin/com/hexated/core/ExtractorHelper.kt:22:17 'constructor(source: String, name: String, url: String, referer: String, quality: Int, headers: Map<String, String> = ..., extractorData: String? = ..., type: ExtractorLinkType, audioTracks: List<AudioFile> = ...): ExtractorLink' is deprecated. Use newExtractorLink.
w: file:///home/runner/work/Aethelion/Aethelion/AsianAnimeProviders/src/main/kotlin/com/hexated/core/ExtractorHelper.kt:38:17 'constructor(source: String, name: String, url: String, referer: String, quality: Int, headers: Map<String, String> = ..., extractorData: String? = ..., type: ExtractorLinkType, audioTracks: List<AudioFile> = ...): ExtractorLink' is deprecated. Use newExtractorLink.
w: file:///home/runner/work/Aethelion/Aethelion/AsianAnimeProviders/src/main/kotlin/com/hexated/extractors/CloseLoad.kt:31:21 'constructor(source: String, name: String, url: String, referer: String, quality: Int, headers: Map<String, String> = ..., extractorData: String? = ..., type: ExtractorLinkType, audioTracks: List<AudioFile> = ...): ExtractorLink' is deprecated. Use newExtractorLink.
w: file:///home/runner/work/Aethelion/Aethelion/AsianAnimeProviders/src/main/kotlin/com/hexated/extractors/FileLions.kt:35:21 'constructor(source: String, name: String, url: String, referer: String, quality: Int, headers: Map<String, String> = ..., extractorData: String? = ..., type: ExtractorLinkType, audioTracks: List<AudioFile> = ...): ExtractorLink' is deprecated. Use newExtractorLink.
w: file:///home/runner/work/Aethelion/Aethelion/AsianAnimeProviders/src/main/kotlin/com/hexated/extractors/Rapidame.kt:35:21 'constructor(source: String, name: String, url: String, referer: String, quality: Int, headers: Map<String, String> = ..., extractorData: String? = ..., type: ExtractorLinkType, audioTracks: List<AudioFile> = ...): ExtractorLink' is deprecated. Use newExtractorLink.
w: file:///home/runner/work/Aethelion/Aethelion/AsianAnimeProviders/src/main/kotlin/com/hexated/extractors/Streamwish.kt:35:21 'constructor(source: String, name: String, url: String, referer: String, quality: Int, headers: Map<String, String> = ..., extractorData: String? = ..., type: ExtractorLinkType, audioTracks: List<AudioFile> = ...): ExtractorLink' is deprecated. Use newExtractorLink.
w: file:///home/runner/work/Aethelion/Aethelion/AsianAnimeProviders/src/main/kotlin/com/hexated/extractors/Vidmoly.kt:35:21 'constructor(source: String, name: String, url: String, referer: String, quality: Int, headers: Map<String, String> = ..., extractorData: String? = ..., type: ExtractorLinkType, audioTracks: List<AudioFile> = ...): ExtractorLink' is deprecated. Use newExtractorLink.

FAILURE: Build failed with an exception.
24 actionable tasks: 24 executed

* What went wrong:
Execution failed for task ':TurkishProviders:compileDebugKotlin'.
> A failure occurred while executing org.jetbrains.kotlin.compilerRunner.GradleCompilerRunnerWithWorkers$GradleKotlinCompilerWorkAction
   > Compilation error. See log for more details

* Try:
> Run with --info or --debug option to get more log output.
> Run with --scan to get full insights.
> Get more help at https://help.gradle.org.

* Exception is:
org.gradle.api.tasks.TaskExecutionException: Execution failed for task ':TurkishProviders:compileDebugKotlin'.
	at org.gradle.api.internal.tasks.execution.ExecuteActionsTaskExecuter.lambda$executeIfValid$1(ExecuteActionsTaskExecuter.java:130)
	at org.gradle.internal.Try$Failure.ifSuccessfulOrElse(Try.java:293)
	at org.gradle.api.internal.tasks.execution.ExecuteActionsTaskExecuter.executeIfValid(ExecuteActionsTaskExecuter.java:128)
	at org.gradle.api.internal.tasks.execution.ExecuteActionsTaskExecuter.execute(ExecuteActionsTaskExecuter.java:116)
	at org.gradle.api.internal.tasks.execution.FinalizePropertiesTaskExecuter.execute(FinalizePropertiesTaskExecuter.java:46)
	at org.gradle.api.internal.tasks.execution.ResolveTaskExecutionModeExecuter.execute(ResolveTaskExecutionModeExecuter.java:51)
	at org.gradle.api.internal.tasks.execution.SkipTaskWithNoActionsExecuter.execute(SkipTaskWithNoActionsExecuter.java:57)
	at org.gradle.api.internal.tasks.execution.SkipOnlyIfTaskExecuter.execute(SkipOnlyIfTaskExecuter.java:74)
	at org.gradle.api.internal.tasks.execution.CatchExceptionTaskExecuter.execute(CatchExceptionTaskExecuter.java:36)
	at org.gradle.api.internal.tasks.execution.EventFiringTaskExecuter$1.executeTask(EventFiringTaskExecuter.java:77)
	at org.gradle.api.internal.tasks.execution.EventFiringTaskExecuter$1.call(EventFiringTaskExecuter.java:55)
	at org.gradle.api.internal.tasks.execution.EventFiringTaskExecuter$1.call(EventFiringTaskExecuter.java:52)
	at org.gradle.internal.operations.DefaultBuildOperationRunner$CallableBuildOperationWorker.execute(DefaultBuildOperationRunner.java:209)
	at org.gradle.internal.operations.DefaultBuildOperationRunner$CallableBuildOperationWorker.execute(DefaultBuildOperationRunner.java:204)
	at org.gradle.internal.operations.DefaultBuildOperationRunner$2.execute(DefaultBuildOperationRunner.java:66)
	at org.gradle.internal.operations.DefaultBuildOperationRunner$2.execute(DefaultBuildOperationRunner.java:59)
	at org.gradle.internal.operations.DefaultBuildOperationRunner.execute(DefaultBuildOperationRunner.java:166)
	at org.gradle.internal.operations.DefaultBuildOperationRunner.execute(DefaultBuildOperationRunner.java:59)
	at org.gradle.internal.operations.DefaultBuildOperationRunner.call(DefaultBuildOperationRunner.java:53)
	at org.gradle.api.internal.tasks.execution.EventFiringTaskExecuter.execute(EventFiringTaskExecuter.java:52)
	at org.gradle.execution.plan.LocalTaskNodeExecutor.execute(LocalTaskNodeExecutor.java:42)
	at org.gradle.execution.taskgraph.DefaultTaskExecutionGraph$InvokeNodeExecutorsAction.execute(DefaultTaskExecutionGraph.java:331)
	at org.gradle.execution.taskgraph.DefaultTaskExecutionGraph$InvokeNodeExecutorsAction.execute(DefaultTaskExecutionGraph.java:318)
	at org.gradle.execution.taskgraph.DefaultTaskExecutionGraph$BuildOperationAwareExecutionAction.lambda$execute$0(DefaultTaskExecutionGraph.java:314)
	at org.gradle.internal.operations.CurrentBuildOperationRef.with(CurrentBuildOperationRef.java:85)
	at org.gradle.execution.taskgraph.DefaultTaskExecutionGraph$BuildOperationAwareExecutionAction.execute(DefaultTaskExecutionGraph.java:314)
	at org.gradle.execution.taskgraph.DefaultTaskExecutionGraph$BuildOperationAwareExecutionAction.execute(DefaultTaskExecutionGraph.java:303)
	at org.gradle.execution.plan.DefaultPlanExecutor$ExecutorWorker.execute(DefaultPlanExecutor.java:459)
	at org.gradle.execution.plan.DefaultPlanExecutor$ExecutorWorker.run(DefaultPlanExecutor.java:376)
	at org.gradle.internal.concurrent.ExecutorPolicy$CatchAndRecordFailures.onExecute(ExecutorPolicy.java:64)
	at org.gradle.internal.concurrent.AbstractManagedExecutor$1.run(AbstractManagedExecutor.java:48)
Caused by: org.gradle.workers.internal.DefaultWorkerExecutor$WorkExecutionException: A failure occurred while executing org.jetbrains.kotlin.compilerRunner.GradleCompilerRunnerWithWorkers$GradleKotlinCompilerWorkAction
	at org.gradle.workers.internal.DefaultWorkerExecutor$WorkItemExecution.waitForCompletion(DefaultWorkerExecutor.java:287)
	at org.gradle.internal.work.DefaultAsyncWorkTracker.lambda$waitForItemsAndGatherFailures$2(DefaultAsyncWorkTracker.java:130)
	at org.gradle.internal.Factories$1.create(Factories.java:31)
	at org.gradle.internal.work.DefaultWorkerLeaseService.withoutLocks(DefaultWorkerLeaseService.java:339)
	at org.gradle.internal.work.DefaultWorkerLeaseService.withoutLocks(DefaultWorkerLeaseService.java:322)
	at org.gradle.internal.work.DefaultWorkerLeaseService.withoutLock(DefaultWorkerLeaseService.java:327)
	at org.gradle.internal.work.DefaultAsyncWorkTracker.waitForItemsAndGatherFailures(DefaultAsyncWorkTracker.java:126)
	at org.gradle.internal.work.DefaultAsyncWorkTracker.waitForItemsAndGatherFailures(DefaultAsyncWorkTracker.java:92)
	at org.gradle.internal.work.DefaultAsyncWorkTracker.waitForAll(DefaultAsyncWorkTracker.java:78)
	at org.gradle.internal.work.DefaultAsyncWorkTracker.waitForCompletion(DefaultAsyncWorkTracker.java:66)
	at org.gradle.api.internal.tasks.execution.TaskExecution$3.run(TaskExecution.java:252)
	at org.gradle.internal.operations.DefaultBuildOperationRunner$1.execute(DefaultBuildOperationRunner.java:29)
	at org.gradle.internal.operations.DefaultBuildOperationRunner$1.execute(DefaultBuildOperationRunner.java:26)
	at org.gradle.internal.operations.DefaultBuildOperationRunner$2.execute(DefaultBuildOperationRunner.java:66)
	at org.gradle.internal.operations.DefaultBuildOperationRunner$2.execute(DefaultBuildOperationRunner.java:59)
	at org.gradle.internal.operations.DefaultBuildOperationRunner.execute(DefaultBuildOperationRunner.java:166)
	at org.gradle.internal.operations.DefaultBuildOperationRunner.execute(DefaultBuildOperationRunner.java:59)
	at org.gradle.internal.operations.DefaultBuildOperationRunner.run(DefaultBuildOperationRunner.java:47)
	at org.gradle.api.internal.tasks.execution.TaskExecution.executeAction(TaskExecution.java:229)
	at org.gradle.api.internal.tasks.execution.TaskExecution.executeActions(TaskExecution.java:212)
	at org.gradle.api.internal.tasks.execution.TaskExecution.executeWithPreviousOutputFiles(TaskExecution.java:195)
	at org.gradle.api.internal.tasks.execution.TaskExecution.execute(TaskExecution.java:162)
	at org.gradle.internal.execution.steps.ExecuteStep.executeInternal(ExecuteStep.java:105)
	at org.gradle.internal.execution.steps.ExecuteStep.access$000(ExecuteStep.java:44)
	at org.gradle.internal.execution.steps.ExecuteStep$1.call(ExecuteStep.java:59)
	at org.gradle.internal.execution.steps.ExecuteStep$1.call(ExecuteStep.java:56)
	at org.gradle.internal.operations.DefaultBuildOperationRunner$CallableBuildOperationWorker.execute(DefaultBuildOperationRunner.java:209)
	at org.gradle.internal.operations.DefaultBuildOperationRunner$CallableBuildOperationWorker.execute(DefaultBuildOperationRunner.java:204)
	at org.gradle.internal.operations.DefaultBuildOperationRunner$2.execute(DefaultBuildOperationRunner.java:66)
	at org.gradle.internal.operations.DefaultBuildOperationRunner$2.execute(DefaultBuildOperationRunner.java:59)
	at org.gradle.internal.operations.DefaultBuildOperationRunner.execute(DefaultBuildOperationRunner.java:166)
	at org.gradle.internal.operations.DefaultBuildOperationRunner.execute(DefaultBuildOperationRunner.java:59)
	at org.gradle.internal.operations.DefaultBuildOperationRunner.call(DefaultBuildOperationRunner.java:53)
	at org.gradle.internal.execution.steps.ExecuteStep.execute(ExecuteStep.java:56)
	at org.gradle.internal.execution.steps.ExecuteStep.execute(ExecuteStep.java:44)
	at org.gradle.internal.execution.steps.CancelExecutionStep.execute(CancelExecutionStep.java:42)
	at org.gradle.internal.execution.steps.TimeoutStep.executeWithoutTimeout(TimeoutStep.java:75)
	at org.gradle.internal.execution.steps.TimeoutStep.execute(TimeoutStep.java:55)
	at org.gradle.internal.execution.steps.PreCreateOutputParentsStep.execute(PreCreateOutputParentsStep.java:50)
	at org.gradle.internal.execution.steps.PreCreateOutputParentsStep.execute(PreCreateOutputParentsStep.java:28)
	at org.gradle.internal.execution.steps.RemovePreviousOutputsStep.execute(RemovePreviousOutputsStep.java:67)
	at org.gradle.internal.execution.steps.RemovePreviousOutputsStep.execute(RemovePreviousOutputsStep.java:37)
	at org.gradle.internal.execution.steps.BroadcastChangingOutputsStep.execute(BroadcastChangingOutputsStep.java:61)
	at org.gradle.internal.execution.steps.BroadcastChangingOutputsStep.execute(BroadcastChangingOutputsStep.java:26)
	at org.gradle.internal.execution.steps.CaptureOutputsAfterExecutionStep.execute(CaptureOutputsAfterExecutionStep.java:69)
	at org.gradle.internal.execution.steps.CaptureOutputsAfterExecutionStep.execute(CaptureOutputsAfterExecutionStep.java:46)
	at org.gradle.internal.execution.steps.ResolveInputChangesStep.execute(ResolveInputChangesStep.java:40)
	at org.gradle.internal.execution.steps.ResolveInputChangesStep.execute(ResolveInputChangesStep.java:29)
	at org.gradle.internal.execution.steps.BuildCacheStep.executeWithoutCache(BuildCacheStep.java:189)
	at org.gradle.internal.execution.steps.BuildCacheStep.executeAndStoreInCache(BuildCacheStep.java:145)
	at org.gradle.internal.execution.steps.BuildCacheStep.lambda$executeWithCache$4(BuildCacheStep.java:101)
	at org.gradle.internal.execution.steps.BuildCacheStep.lambda$executeWithCache$5(BuildCacheStep.java:101)
	at org.gradle.internal.Try$Success.map(Try.java:175)
	at org.gradle.internal.execution.steps.BuildCacheStep.executeWithCache(BuildCacheStep.java:85)
	at org.gradle.internal.execution.steps.BuildCacheStep.lambda$execute$0(BuildCacheStep.java:74)
	at org.gradle.internal.Either$Left.fold(Either.java:115)
	at org.gradle.internal.execution.caching.CachingState.fold(CachingState.java:62)
	at org.gradle.internal.execution.steps.BuildCacheStep.execute(BuildCacheStep.java:73)
	at org.gradle.internal.execution.steps.BuildCacheStep.execute(BuildCacheStep.java:48)
	at org.gradle.internal.execution.steps.StoreExecutionStateStep.execute(StoreExecutionStateStep.java:46)
	at org.gradle.internal.execution.steps.StoreExecutionStateStep.execute(StoreExecutionStateStep.java:35)
	at org.gradle.internal.execution.steps.SkipUpToDateStep.executeBecause(SkipUpToDateStep.java:75)
	at org.gradle.internal.execution.steps.SkipUpToDateStep.lambda$execute$2(SkipUpToDateStep.java:53)
	at org.gradle.internal.execution.steps.SkipUpToDateStep.execute(SkipUpToDateStep.java:53)
	at org.gradle.internal.execution.steps.SkipUpToDateStep.execute(SkipUpToDateStep.java:35)
	at org.gradle.internal.execution.steps.legacy.MarkSnapshottingInputsFinishedStep.execute(MarkSnapshottingInputsFinishedStep.java:37)
	at org.gradle.internal.execution.steps.legacy.MarkSnapshottingInputsFinishedStep.execute(MarkSnapshottingInputsFinishedStep.java:27)
	at org.gradle.internal.execution.steps.ResolveIncrementalCachingStateStep.executeDelegate(ResolveIncrementalCachingStateStep.java:49)
	at org.gradle.internal.execution.steps.ResolveIncrementalCachingStateStep.executeDelegate(ResolveIncrementalCachingStateStep.java:27)
	at org.gradle.internal.execution.steps.AbstractResolveCachingStateStep.execute(AbstractResolveCachingStateStep.java:71)
	at org.gradle.internal.execution.steps.AbstractResolveCachingStateStep.execute(AbstractResolveCachingStateStep.java:39)
	at org.gradle.internal.execution.steps.ResolveChangesStep.execute(ResolveChangesStep.java:65)
	at org.gradle.internal.execution.steps.ResolveChangesStep.execute(ResolveChangesStep.java:36)
	at org.gradle.internal.execution.steps.ValidateStep.execute(ValidateStep.java:105)
	at org.gradle.internal.execution.steps.ValidateStep.execute(ValidateStep.java:54)
	at org.gradle.internal.execution.steps.AbstractCaptureStateBeforeExecutionStep.execute(AbstractCaptureStateBeforeExecutionStep.java:64)
	at org.gradle.internal.execution.steps.AbstractCaptureStateBeforeExecutionStep.execute(AbstractCaptureStateBeforeExecutionStep.java:43)
	at org.gradle.internal.execution.steps.AbstractSkipEmptyWorkStep.executeWithNonEmptySources(AbstractSkipEmptyWorkStep.java:125)
	at org.gradle.internal.execution.steps.AbstractSkipEmptyWorkStep.execute(AbstractSkipEmptyWorkStep.java:61)
	at org.gradle.internal.execution.steps.AbstractSkipEmptyWorkStep.execute(AbstractSkipEmptyWorkStep.java:36)
	at org.gradle.internal.execution.steps.legacy.MarkSnapshottingInputsStartedStep.execute(MarkSnapshottingInputsStartedStep.java:38)
	at org.gradle.internal.execution.steps.LoadPreviousExecutionStateStep.execute(LoadPreviousExecutionStateStep.java:36)
	at org.gradle.internal.execution.steps.LoadPreviousExecutionStateStep.execute(LoadPreviousExecutionStateStep.java:23)
	at org.gradle.internal.execution.steps.HandleStaleOutputsStep.execute(HandleStaleOutputsStep.java:75)
	at org.gradle.internal.execution.steps.HandleStaleOutputsStep.execute(HandleStaleOutputsStep.java:41)
	at org.gradle.internal.execution.steps.AssignMutableWorkspaceStep.lambda$execute$0(AssignMutableWorkspaceStep.java:35)
	at org.gradle.api.internal.tasks.execution.TaskExecution$4.withWorkspace(TaskExecution.java:289)
	at org.gradle.internal.execution.steps.AssignMutableWorkspaceStep.execute(AssignMutableWorkspaceStep.java:31)
	at org.gradle.internal.execution.steps.AssignMutableWorkspaceStep.execute(AssignMutableWorkspaceStep.java:22)
	at org.gradle.internal.execution.steps.ChoosePipelineStep.execute(ChoosePipelineStep.java:40)
	at org.gradle.internal.execution.steps.ChoosePipelineStep.execute(ChoosePipelineStep.java:23)
	at org.gradle.internal.execution.steps.ExecuteWorkBuildOperationFiringStep.lambda$execute$2(ExecuteWorkBuildOperationFiringStep.java:67)
	at org.gradle.internal.execution.steps.ExecuteWorkBuildOperationFiringStep.execute(ExecuteWorkBuildOperationFiringStep.java:67)
	at org.gradle.internal.execution.steps.ExecuteWorkBuildOperationFiringStep.execute(ExecuteWorkBuildOperationFiringStep.java:39)
	at org.gradle.internal.execution.steps.IdentityCacheStep.execute(IdentityCacheStep.java:46)
	at org.gradle.internal.execution.steps.IdentityCacheStep.execute(IdentityCacheStep.java:34)
	at org.gradle.internal.execution.steps.IdentifyStep.execute(IdentifyStep.java:48)
	at org.gradle.internal.execution.steps.IdentifyStep.execute(IdentifyStep.java:35)
	at org.gradle.internal.execution.impl.DefaultExecutionEngine$1.execute(DefaultExecutionEngine.java:61)
	at org.gradle.api.internal.tasks.execution.ExecuteActionsTaskExecuter.executeIfValid(ExecuteActionsTaskExecuter.java:127)
	at org.gradle.api.internal.tasks.execution.ExecuteActionsTaskExecuter.execute(ExecuteActionsTaskExecuter.java:116)
	at org.gradle.api.internal.tasks.execution.FinalizePropertiesTaskExecuter.execute(FinalizePropertiesTaskExecuter.java:46)
	at org.gradle.api.internal.tasks.execution.ResolveTaskExecutionModeExecuter.execute(ResolveTaskExecutionModeExecuter.java:51)
	at org.gradle.api.internal.tasks.execution.SkipTaskWithNoActionsExecuter.execute(SkipTaskWithNoActionsExecuter.java:57)
	at org.gradle.api.internal.tasks.execution.SkipOnlyIfTaskExecuter.execute(SkipOnlyIfTaskExecuter.java:74)
	at org.gradle.api.internal.tasks.execution.CatchExceptionTaskExecuter.execute(CatchExceptionTaskExecuter.java:36)
	at org.gradle.api.internal.tasks.execution.EventFiringTaskExecuter$1.executeTask(EventFiringTaskExecuter.java:77)
	at org.gradle.api.internal.tasks.execution.EventFiringTaskExecuter$1.call(EventFiringTaskExecuter.java:55)
	at org.gradle.api.internal.tasks.execution.EventFiringTaskExecuter$1.call(EventFiringTaskExecuter.java:52)
	at org.gradle.internal.operations.DefaultBuildOperationRunner$CallableBuildOperationWorker.execute(DefaultBuildOperationRunner.java:209)
	at org.gradle.internal.operations.DefaultBuildOperationRunner$CallableBuildOperationWorker.execute(DefaultBuildOperationRunner.java:204)
	at org.gradle.internal.operations.DefaultBuildOperationRunner$2.execute(DefaultBuildOperationRunner.java:66)
	at org.gradle.internal.operations.DefaultBuildOperationRunner$2.execute(DefaultBuildOperationRunner.java:59)
	at org.gradle.internal.operations.DefaultBuildOperationRunner.execute(DefaultBuildOperationRunner.java:166)
	at org.gradle.internal.operations.DefaultBuildOperationRunner.execute(DefaultBuildOperationRunner.java:59)
	at org.gradle.internal.operations.DefaultBuildOperationRunner.call(DefaultBuildOperationRunner.java:53)
	at org.gradle.api.internal.tasks.execution.EventFiringTaskExecuter.execute(EventFiringTaskExecuter.java:52)
	at org.gradle.execution.plan.LocalTaskNodeExecutor.execute(LocalTaskNodeExecutor.java:42)
	at org.gradle.execution.taskgraph.DefaultTaskExecutionGraph$InvokeNodeExecutorsAction.execute(DefaultTaskExecutionGraph.java:331)
	at org.gradle.execution.taskgraph.DefaultTaskExecutionGraph$InvokeNodeExecutorsAction.execute(DefaultTaskExecutionGraph.java:318)
	at org.gradle.execution.taskgraph.DefaultTaskExecutionGraph$BuildOperationAwareExecutionAction.lambda$execute$0(DefaultTaskExecutionGraph.java:314)
	at org.gradle.internal.operations.CurrentBuildOperationRef.with(CurrentBuildOperationRef.java:85)
	at org.gradle.execution.taskgraph.DefaultTaskExecutionGraph$BuildOperationAwareExecutionAction.execute(DefaultTaskExecutionGraph.java:314)
	at org.gradle.execution.taskgraph.DefaultTaskExecutionGraph$BuildOperationAwareExecutionAction.execute(DefaultTaskExecutionGraph.java:303)
	at org.gradle.execution.plan.DefaultPlanExecutor$ExecutorWorker.execute(DefaultPlanExecutor.java:459)
	at org.gradle.execution.plan.DefaultPlanExecutor$ExecutorWorker.run(DefaultPlanExecutor.java:376)
	at org.gradle.internal.concurrent.ExecutorPolicy$CatchAndRecordFailures.onExecute(ExecutorPolicy.java:64)
	at org.gradle.internal.concurrent.AbstractManagedExecutor$1.run(AbstractManagedExecutor.java:48)
Caused by: org.jetbrains.kotlin.gradle.tasks.CompilationErrorException: Compilation error. See log for more details
	at org.jetbrains.kotlin.gradle.tasks.TasksUtilsKt.throwExceptionIfCompilationFailed(tasksUtils.kt:21)
	at org.jetbrains.kotlin.compilerRunner.GradleKotlinCompilerWork.run(GradleKotlinCompilerWork.kt:119)
	at org.jetbrains.kotlin.compilerRunner.GradleCompilerRunnerWithWorkers$GradleKotlinCompilerWorkAction.execute(GradleCompilerRunnerWithWorkers.kt:76)
	at org.gradle.workers.internal.DefaultWorkerServer.execute(DefaultWorkerServer.java:63)
	at org.gradle.workers.internal.NoIsolationWorkerFactory$1$1.create(NoIsolationWorkerFactory.java:66)
	at org.gradle.workers.internal.NoIsolationWorkerFactory$1$1.create(NoIsolationWorkerFactory.java:62)
	at org.gradle.internal.classloader.ClassLoaderUtils.executeInClassloader(ClassLoaderUtils.java:100)
	at org.gradle.workers.internal.NoIsolationWorkerFactory$1.lambda$execute$0(NoIsolationWorkerFactory.java:62)
	at org.gradle.workers.internal.AbstractWorker$1.call(AbstractWorker.java:44)
	at org.gradle.workers.internal.AbstractWorker$1.call(AbstractWorker.java:41)
	at org.gradle.internal.operations.DefaultBuildOperationRunner$CallableBuildOperationWorker.execute(DefaultBuildOperationRunner.java:209)
	at org.gradle.internal.operations.DefaultBuildOperationRunner$CallableBuildOperationWorker.execute(DefaultBuildOperationRunner.java:204)
	at org.gradle.internal.operations.DefaultBuildOperationRunner$2.execute(DefaultBuildOperationRunner.java:66)
	at org.gradle.internal.operations.DefaultBuildOperationRunner$2.execute(DefaultBuildOperationRunner.java:59)
	at org.gradle.internal.operations.DefaultBuildOperationRunner.execute(DefaultBuildOperationRunner.java:166)
	at org.gradle.internal.operations.DefaultBuildOperationRunner.execute(DefaultBuildOperationRunner.java:59)
	at org.gradle.internal.operations.DefaultBuildOperationRunner.call(DefaultBuildOperationRunner.java:53)
	at org.gradle.workers.internal.AbstractWorker.executeWrappedInBuildOperation(AbstractWorker.java:41)
	at org.gradle.workers.internal.NoIsolationWorkerFactory$1.execute(NoIsolationWorkerFactory.java:59)
	at org.gradle.workers.internal.DefaultWorkerExecutor.lambda$submitWork$0(DefaultWorkerExecutor.java:174)
	at org.gradle.internal.work.DefaultConditionalExecutionQueue$ExecutionRunner.runExecution(DefaultConditionalExecutionQueue.java:195)
	at org.gradle.internal.work.DefaultConditionalExecutionQueue$ExecutionRunner.access$700(DefaultConditionalExecutionQueue.java:128)
	at org.gradle.internal.work.DefaultConditionalExecutionQueue$ExecutionRunner$1.run(DefaultConditionalExecutionQueue.java:170)
	at org.gradle.internal.Factories$1.create(Factories.java:31)
	at org.gradle.internal.work.DefaultWorkerLeaseService.withLocks(DefaultWorkerLeaseService.java:267)
	at org.gradle.internal.work.DefaultWorkerLeaseService.runAsWorkerThread(DefaultWorkerLeaseService.java:131)
	at org.gradle.internal.work.DefaultWorkerLeaseService.runAsWorkerThread(DefaultWorkerLeaseService.java:136)
	at org.gradle.internal.work.DefaultConditionalExecutionQueue$ExecutionRunner.runBatch(DefaultConditionalExecutionQueue.java:165)
	at org.gradle.internal.work.DefaultConditionalExecutionQueue$ExecutionRunner.run(DefaultConditionalExecutionQueue.java:134)
	... 2 more


BUILD FAILED in 1m 28s
Error: Process completed with exit code 1.