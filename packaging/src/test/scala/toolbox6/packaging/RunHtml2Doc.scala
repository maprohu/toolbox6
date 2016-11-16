package toolbox6.packaging

import java.io.File

import org.docx4j.XmlUtils
import org.docx4j.convert.in.xhtml.XHTMLImporterImpl
import org.docx4j.openpackaging.packages.WordprocessingMLPackage
import sbt.io.IO

/**
  * Created by pappmar on 16/11/2016.
  */
object RunHtml2Doc {

//  val xhtml =
//      "<table border=\"1\" cellpadding=\"1\" cellspacing=\"1\" style=\"width:100%;\"><tbody><tr><td>test</td><td>test</td></tr><tr><td>test</td><td>test</td></tr><tr><td>test</td><td>test</td></tr></tbody></table>"


  val xhtml =
    """
       <html><head><link href="META-INF/resources/webjars/font-awesome/4.2.0/css/font-awesome.min.css" rel="stylesheet" /><link href="styles.css" rel="stylesheet" /><link rel="shortcut icon" type="image/png" href="favicon.png" /><meta http-equiv="Content-Type" content="text/html; charset=UTF-8" /><style>
.scalatex-site-Styles-headerLink{
  color: #777;
  opacity: 0.05;
  text-decoration: none;
}


.scalatex-site-Styles-hoverContainer:hover .scalatex-site-Styles-headerLink{
  color: #777;
  opacity: 0.5;
  text-decoration: none;
}
.scalatex-site-Styles-hoverContainer:hover .scalatex-site-Styles-headerLink:hover{
  opacity: 1.0;
}
.scalatex-site-Styles-hoverContainer:hover .scalatex-site-Styles-headerLink:active{
  opacity: 0.75;
}

.scalatex-site-Styles-content{
  color: #777;
  line-height: 1.6em;
  margin: 0 auto;
  margin-left: auto;
  margin-right: auto;
  max-width: 800px;
  padding: 0 1em;
  padding-bottom: 50px;
}
.scalatex-site-Styles-content *{
  position: relative;
}
.scalatex-site-Styles-content p{
  text-align: justify;
}
.scalatex-site-Styles-content a:link{
  color: #37a;
  text-decoration: none;
}
.scalatex-site-Styles-content a:visited{
  color: #949;
  text-decoration: none;
}
.scalatex-site-Styles-content a:hover{
  text-decoration: underline;
}
.scalatex-site-Styles-content a:active{
  color: #000;
  text-decoration: underline;
}
.scalatex-site-Styles-content code{
  color: #000;
}

/*Workaround for bug in highlight.js IDEA theme*/
span.hljs-tag, span.hljs-symbol{
    background: none;
}
    </style><title>Wupdata-Store Installation Manual</title><script src="scripts.js"></script></head><body><div><div style="margin: 0px;color: #333;text-align: center;padding: 2.5em 2em 0;border-bottom: 1px solid #eee;display: block;" id="Wupdata-StoreInstallationManual" class=" scalatex-site-Styles-hoverContainer scalatex-site-Styles-headerTag"><h1 style="margin: 0.2em 0;font-size: 3em;font-weight: 300;">Wupdata-Store Installation Manual<a class=" scalatex-site-Styles-headerLink" href="#Wupdata-StoreInstallationManual" style="position: absolute;right: 0px;"><i class="fa fa-link"></i></a></h1><br /><h2 style="font-weight: 300;color: #ccc;padding: 0px;margin-top: 0px;">Version 2.1.0-RC1</h2></div><div class=" scalatex-site-Styles-content"><p>
              The <i>Wupdata-Store</i> distribution can be downloaded from TeamForge: <br />
              <a href="https://sf.emsa.europa.eu/sf/frs/do/listReleases/projects.imdate/frs.wup_configuration">https://sf.emsa.europa.eu/sf/frs/do/listReleases/projects.imdate/frs.wup_configuration</a>
         </p><p>
            The application is distributed as a self-installing package.
            The installation can be performed by running the following command:
         </p><pre>
              java -jar wupdata-installer.jar &lt;target&gt;
            </pre><p>
              where <i>&lt;target&gt;</i> is one of the following:
            <ul><li style="font-family: monospace;">test</li><li style="font-family: monospace;">preprod</li><li style="font-family: monospace;">prod</li></ul></p><p>
              The installer will perform the following steps:
            <ul><li>ask for the password of the weblogic admin user (if not known)</li><li>connect to the Weblogic administration server in the target environment</li><li>stop and undeploy any previous versions of the <i>wupdata</i> application</li><li>deploy and start the new version of the application by the name <b>wupdata_2.1.0-RC1</b></li></ul></p><p>The installer uses the following values:<table border="1" style="width: 100%;"><tr><th></th><th>test</th><th>preprod</th><th>prod</th></tr><tr><th>AdminServer Address</th><td style="padding: 5px;"><i>twls51</i></td><td style="padding: 5px;"><i>qwls51</i></td><td style="padding: 5px;"><i>pwls51</i></td></tr><tr><th>AdminServer Port</th><td style="padding: 5px;"><i>7203</i></td><td style="padding: 5px;"><i>7203</i></td><td style="padding: 5px;"><i>7203</i></td></tr><tr><th>AdminServer User</th><td style="padding: 5px;"><i>weblogic</i></td><td style="padding: 5px;"><i>weblogic</i></td><td style="padding: 5px;"><i>weblogic</i></td></tr><tr><th>AdminServer Password</th><td style="padding: 5px;"><i>weblogic1</i></td><td style="padding: 5px;"><i>&lt;will ask user&gt;</i></td><td style="padding: 5px;"><i>&lt;will ask user&gt;</i></td></tr><tr><th>Deployment Target</th><td style="padding: 5px;"><i>imdateAppCluster</i></td><td style="padding: 5px;"><i>imdateAppCluster</i></td><td style="padding: 5px;"><i>imdateAppCluster</i></td></tr></table></p></div></div><script>
    scalatex.scrollspy.Controller().main(
      [{"value":"Wupdata-Store Installation Manual","children":[]}]
  )</script><script>
    ['DOMContentLoaded', 'load'].forEach(function(ev){
      addEventListener(ev, function(){
        Array.prototype.forEach.call(
          document.getElementsByClassName('scalatex-site-Styles-highlightMe'),
          hljs.highlightBlock
        );
      })
    })
  </script></body></html>

    """

  def main(args: Array[String]): Unit = {
    val base = new File("../emsa-managed/target/docs/wupdata_2.1.0-RC1_installation_manual/index.html")

//    val xhtml =
//      IO.read(
//        new File("../emsa-managed/target/docs/wupdata_2.1.0-RC1_installation_manual/index.html")
//      )

    val wordMLPackage = WordprocessingMLPackage.createPackage()
    val XHTMLImporter = new XHTMLImporterImpl(wordMLPackage);

    wordMLPackage.getMainDocumentPart().getContent().addAll(
      XHTMLImporter.convert( xhtml, base.toURI.toASCIIString) );

    wordMLPackage.save(
      new File("../toolbox6/target/test.docx")
    )
  }

}
