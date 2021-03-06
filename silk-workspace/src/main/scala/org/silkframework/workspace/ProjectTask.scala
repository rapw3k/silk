/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.silkframework.workspace

import java.util.concurrent.{Executors, ScheduledFuture, TimeUnit}
import java.util.logging.{Level, Logger}

import org.silkframework.config.{Task, TaskSpec}
import org.silkframework.runtime.activity.{HasValue, Status}
import org.silkframework.runtime.plugin.PluginRegistry
import org.silkframework.config.TaskSpec
import org.silkframework.util.Identifier
import org.silkframework.workspace.activity.{TaskActivity, TaskActivityFactory}

import scala.reflect.ClassTag
import scala.util.control.NonFatal


/**
 * A task that belongs to a project.
 *
 * @tparam TaskType The data type that specifies the properties of this task.
 */
class ProjectTask[TaskType <: TaskSpec : ClassTag](val id: Identifier, initialData: TaskType,
                                                   module: Module[TaskType]) {

  private val log = Logger.getLogger(getClass.getName)

  @volatile
  private var currentData: TaskType = initialData

  @volatile
  private var scheduledWriter: Option[ScheduledFuture[_]] = None

  private val taskActivities: Seq[TaskActivity[TaskType, _ <: HasValue]] = {
    // Get all task activity factories for this task type
    implicit val prefixes = module.project.config.prefixes
    implicit val resources = module.project.resources
    val factories = PluginRegistry.availablePlugins[TaskActivityFactory[TaskType, _ <: HasValue]].map(_.apply()).filter(_.isTaskType[TaskType])
    var activities = List[TaskActivity[TaskType, _ <: HasValue]]()
    for(factory <- factories) {
      try {
        activities ::= new TaskActivity(this, factory)
      } catch {
        case NonFatal(ex) =>
          log.log(Level.WARNING, s"Could not load task activity '$factory' in task '$id' in project '${module.project.name}'.", ex)
      }
    }
    activities.reverse
  }

  private val taskActivityMap: Map[Class[_], TaskActivity[TaskType, _ <: HasValue]] = taskActivities.map(a => (a.activityType, a)).toMap

  /**
    * The project this task belongs to.
    */
  def project = module.project

  /**
   * Retrieves the current data of this task.
   */
  def data = currentData

  def task = Task(id, currentData)

  def init() = {
    // Start autorun activities
    for(activity <- taskActivities if activity.autoRun && activity.status == Status.Idle())
      activity.control.start()
  }

  /**
   * Updates the data of this task.
   */
  def update(newData: TaskType) = synchronized {
    // Update data
    currentData = newData
    // (Re)Schedule write
    for(writer <- scheduledWriter) {
      writer.cancel(false)
    }
    scheduledWriter = Some(ProjectTask.scheduledExecutor.schedule(Writer, ProjectTask.writeInterval, TimeUnit.SECONDS))
    log.info("Updated task '" + id + "'")
  }

  /**
   * All activities that belong to this task.
   */
  def activities: Seq[TaskActivity[TaskType, _]] = taskActivities

  /**
   * Retrieves an activity by type.
   *
   * @tparam T The type of the requested activity
   * @return The activity control for the requested activity
   */
  def activity[T <: HasValue : ClassTag]: TaskActivity[TaskType, T] = {
    val requestedClass = implicitly[ClassTag[T]].runtimeClass
    val act = taskActivityMap.getOrElse(requestedClass, throw new NoSuchElementException(s"Task '$id' in project '${project.name}' does not contain an activity of type '${requestedClass.getName}'. " +
                                                                                         s"Available activities:\n${taskActivityMap.keys.map(_.getName).mkString("\n ")}"))
    act.asInstanceOf[TaskActivity[TaskType, T]]
  }

  /**
   * Retrieves an activity by name.
   *
   * @param activityName The name of the requested activity
   * @return The activity control for the requested activity
   */
  def activity(activityName: String): TaskActivity[TaskType, _ <: HasValue] = {
    taskActivities.find(_.name == activityName)
      .getOrElse(throw new NoSuchElementException(s"Task '$id' in project '${project.name}' does not contain an activity named '$activityName'. " +
                                                  s"Available activities: ${taskActivityMap.values.map(_.name).mkString(", ")}"))
  }

  private object Writer extends Runnable {
    override def run(): Unit = {
      // Write task
      module.provider.putTask(project.name, id, data)
      log.info(s"Persisted task '$id' in project '${project.name}'")
      // Update caches
      for(activity <- taskActivities if activity.autoRun) {
        activity.control.cancel()
        activity.control.start()
      }
    }
  }
}

object ProjectTask {

  /* Do not persist updates more frequently than this (in seconds) */
  private val writeInterval = 5

  private val scheduledExecutor = Executors.newSingleThreadScheduledExecutor()

}