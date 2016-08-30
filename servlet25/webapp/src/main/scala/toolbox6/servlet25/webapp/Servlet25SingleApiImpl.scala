package toolbox6.servlet25.webapp

import toolbox6.common.SingleRegistry
import toolbox6.servlet25.singleapi.{Servlet25SingleApi, Servlet25SingleHandler}


/**
  * Created by pappmar on 30/08/2016.
  */
class Servlet25SingleApiImpl extends SingleRegistry[Servlet25SingleHandler] with Servlet25SingleApi

