package org.silkframework.runtime.activity

import java.util.logging.Logger

import org.silkframework.runtime.activity.Status.Canceling

private class ActivityExecution[T](activity: Activity[T],
                                   parent: Option[ActivityContext[_]] = None,
                                   progressContribution: Double = 0.0) extends Runnable with ActivityControl[T] with ActivityContext[T] {

  /**
   * The name of the activity.
   */
  override val name: String = activity.name

  /**
    * The logger to be used by the activity.
    */
  override val log = parent match {
    case None => Logger.getLogger(Activity.loggingPath + "." + name)
    case Some(p) => Logger.getLogger(p.log.getName + "." + name)
  }

  /**
   * Holds the current value.
   */
  override val value = new ValueHolder[T](activity.initialValue)

  /**
   * Holds the current status.
   */
  override val status = new StatusHolder(log, parent.map(_.status), progressContribution)

  @volatile
  private var user: UserContext = UserContext.Empty

  @volatile
  private var childControls: Seq[ActivityControl[_]] = Seq.empty

  override def run(): Unit = synchronized {
    if(!parent.exists(_.status().isInstanceOf[Canceling])) {
      val startTime = System.currentTimeMillis
      try {
        activity.run(this)
        status.update(Status.Finished(success = true, System.currentTimeMillis - startTime))
      } catch {
        case ex: Throwable =>
          status.update(Status.Finished(success = false, System.currentTimeMillis - startTime, Some(ex)))
          throw ex
      }
    }
  }

  override def children(): Seq[ActivityControl[_]] = {
    removeDoneChildren()
    childControls
  }

  override def start()(implicit user: UserContext): Unit = {
    // Check if the current activity is still running
    if(status().isRunning)
      throw new IllegalStateException(s"Cannot start while activity ${this.activity.name} is still running!")
    // Execute activity
    this.user = user
    status.update(Status.Started())
    Activity.executionContext.execute(this)
  }

  override def startBlocking()(implicit user: UserContext): Unit = {
    this.user = user
    status.update(Status.Started())
    run()
  }

  override def startBlockingAndGetValue(initialValue: Option[T])(implicit user: UserContext): T = {
    this.user = user
    status.update(Status.Started())
    for(v <- initialValue)
      value.update(v)
    run()
    value()
  }

  override def cancel() = {
    if(status().isRunning && !status().isInstanceOf[Status.Canceling]) {
      status.update(Status.Canceling(status().progress))
      childControls.foreach(_.cancel())
      activity.cancelExecution()
    }
  }

  override def reset() = {
    activity.initialValue.foreach(value.update)
    activity.reset()
  }

  override def child[R](activity: Activity[R], progressContribution: Double = 0.0): ActivityControl[R] = {
    val execution = new ActivityExecution(activity, Some(this), progressContribution)
    addChild(execution)
    execution
  }

  override def underlying: Activity[T] = activity

  private def addChild(control: ActivityControl[_]): Unit = {
    childControls = childControls :+ control
  }

  private def removeDoneChildren(): Unit = {
    childControls = childControls.filter(_.status().isRunning)
  }
}