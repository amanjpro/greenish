package me.amanj.greenish.checker

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ ImplicitSender, TestKit }
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class CommandRunnerSpec()
    extends TestKit(ActorSystem("CommandRunnerSpec"))
    with ImplicitSender
    with AnyWordSpecLike
    with Matchers
    with BeforeAndAfterAll {

  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "CommandRunnerSpec Actor" must {

    "send back false, when command does not exit with 0" in {
      val echo = system.actorOf(Props(new CommandRunner()))
      echo ! Run("false", Seq.empty)
      expectMsg(false)
    }

    "send back true, when command does exit with 0" in {
      val echo = system.actorOf(Props(new CommandRunner()))
      echo ! Run("true", Seq.empty)
      expectMsg(true)
    }

    "use provided environment variables" in {
      val scriptPath = getClass.getResource("/test-env").getFile
      val echo = system.actorOf(Props(new CommandRunner()))
      echo ! Run(scriptPath, Seq.empty)
      expectMsg(false)

      echo ! Run(scriptPath, Seq("GREENISH_VALUE_FOR_TEST" -> "."))
      expectMsg(true)
    }
  }
}
