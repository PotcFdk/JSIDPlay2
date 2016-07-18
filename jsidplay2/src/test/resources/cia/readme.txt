<!DOCTYPE html>
<!-- Server: sfn-web-10 -->


    


















<!--[if lt IE 7 ]> <html lang="en" class="no-js ie6"> <![endif]-->
<!--[if IE 7 ]>    <html lang="en" class="no-js ie7"> <![endif]-->
<!--[if IE 8 ]>    <html lang="en" class="no-js ie8"> <![endif]-->
<!--[if IE 9 ]>    <html lang="en" class="no-js ie9"> <![endif]-->
<!--[if (gt IE 9)|!(IE)]>-->
<html lang="en" class="no-js"> <!--<![endif]-->
<head>
    <meta content="text/html; charset=UTF-8" http-equiv="content-type"/>
    <title>
  VICE / Code /
  [r31403]
  /testprogs/CIA/dd0dtest/readme.txt
</title>
    


<script type="text/javascript">
  var _paq = _paq || [];
  _paq.push(['trackPageView']);
  _paq.push(['enableLinkTracking']);
  (function() {
    var u="//analytics.slashdotmedia.com/";
    _paq.push(['setTrackerUrl', u+'sf.php']);
    _paq.push(['setSiteId', 39]);
    var d=document, g=d.createElement('script'), s=d.getElementsByTagName('script')[0];
    g.type='text/javascript'; g.async=true; g.defer=true; g.src=u+'sf.js'; s.parentNode.insertBefore(g,s);
  })();
</script>
<noscript><p><img src="//analytics.slashdotmedia.com/sf.php?idsite=39" style="border:0;" alt="" /></p></noscript>

<meta id="project_name" name="project_name" content='vice-emu' />
<!--[if lt IE 7 ]>
  <script src="https://a.fsdn.com/allura/nf/1468509372/_ew_/theme/sftheme/js/sftheme/vendor/dd_belatedpng.js"></script>
  <script> DD_belatedPNG.fix('img, .png_bg'); //fix any <img> or .png_bg background-images </script>
<![endif]-->
<link href='//fonts.googleapis.com/css?family=Ubuntu:regular' rel='stylesheet' type='text/css'>
    
        <!-- ew:head_css -->

    
        <link rel="stylesheet"
                type="text/css"
                href="https://a.fsdn.com/allura/nf/1468509372/_ew_/_slim/css?href=allura%2Fcss%2Fforge%2Fhilite.css%3Ballura%2Fcss%2Fforge%2Ftooltipster.css"
                >
    
        <link rel="stylesheet"
                type="text/css"
                href="https://a.fsdn.com/allura/nf/1468509372/_ew_/allura/css/font-awesome.min.css"
                >
    
        <link rel="stylesheet"
                type="text/css"
                href="https://a.fsdn.com/allura/nf/1468509372/_ew_/theme/sftheme/css/forge.css"
                >
    
        
<!-- /ew:head_css -->

    
    
        <!-- ew:head_js -->

    
        <script type="text/javascript" src="https://a.fsdn.com/allura/nf/1468509372/_ew_/_slim/js?href=allura%2Fjs%2Fjquery-base.js%3Btheme%2Fsftheme%2Fjs%2Fsftheme%2Fvendor%2Fmodernizr.custom.90514.js%3Btheme%2Fsftheme%2Fjs%2Fsftheme%2Fshared_head.js"></script>
    
        
<!-- /ew:head_js -->

    

    
        <style type="text/css">
            #page-body.project---init-- #top_nav { display: none; }

#page-body.project---init-- #nav_menu_holder { display: none; margin-bottom: 0; }

#page-body.project---init-- #content_base {margin-top: 0; }
        </style>
    
    
    <link rel="alternate" type="application/rss+xml" title="RSS" href="/p/vice-emu/code/feed.rss"/>
    <link rel="alternate" type="application/atom+xml" title="Atom" href="/p/vice-emu/code/feed.atom"/>

    <style>.XZYLidHxwVpPKiyTmGMPlTNw {
        display: none
    }</style>

    
    
    
    


<script type="text/javascript">
    (function(i,s,o,g,r,a,m){i['GoogleAnalyticsObject']=r;i[r]=i[r]||function(){
            (i[r].q=i[r].q||[]).push(arguments)},i[r].l=1*new Date();a=s.createElement(o),
        m=s.getElementsByTagName(o)[0];a.async=1;a.src=g;m.parentNode.insertBefore(a,m)
    })(window,document,'script','//www.google-analytics.com/analytics.js','ga');

    function _add_tracking(prefix, tracking_id) {
        ga('create', tracking_id, {cookieDomain: 'auto', 'name': prefix});
        
        ga(prefix+'.set', 'dimension9', 'vice-emu');
        ga(prefix+'.set', 'dimension10', 'svn');
        
        ga(prefix+'.set', 'dimension13', 'Logged Out');
        ga(prefix+'.send', 'pageview');
    }
      _add_tracking('sfnt1', 'UA-32013-6');
      _add_tracking('sfnt2', 'UA-36130941-1');
    
</script>
</head>

<body id="forge">

    
        <!-- ew:body_top_js -->

    
        
<!-- /ew:body_top_js -->

    


<header id="site-header">
    <div class="wrapper">
        <a href="/" class="logo">
            <span>SourceForge</span>
        </a>
        
        <form method="get" action="/directory/">
            <input type="text" id="words" name="q" placeholder="Search">
        </form>
        
        <!--Switch to {language}-->
        <nav id="nav-site">
            <a href="/directory/" title="Browse our software.">Browse</a>
            <a href="/directory/enterprise" title="Browse our Enterprise software.">Enterprise</a>
            <a href="/blog/" title="Read the latest news from the SF HQ.">Blog</a>
            <a href="//deals.sourceforge.net/?utm_source=sourceforge&amp;utm_medium=navbar&amp;utm_campaign=homepage" title="Discover and Save on the Best Gear, Gadgets, and Software" class="featured-link" target="_blank">Deals</a>
            <a href="/support" title="Contact us for help and feedback.">Help</a>
            <a href="/create"  class="featured-link blue"  style="" title="Create and publish Open Source software for free.">Create</a>
        </nav>
        <nav id="nav-account">
            
              <div class="logged_out">
                <a href="/auth/">Log In</a>
                <span>or</span>
                <a href="https://sourceforge.net/user/registration/">Join</a>
              </div>
            
        </nav>
        
    </div>
</header>
<header id="site-sec-header">
    <div class="wrapper">
        <nav id="nav-hubs">
            <h4>Solution Centers</h4>
            <a href="http://goparallel.sourceforge.net/">Go Parallel</a>
        </nav>
        <nav id="nav-collateral">
            <a href="https://library.slashdotmedia.com/">Resources</a>
            <a href="/user/newsletters?source=sfnet_header">Newsletters</a>
            <a href="/cloud-storage-providers/?source=sfnet_header">Cloud Storage Providers</a>
            <a href="/business-voip/?source=sfnet_header">Business VoIP Providers</a>
            
            <a href="/call-center-providers/?source=sfnet_header">Call Center Providers</a>
        </nav>
    </div>
</header>

    
    
    

<section id="page-body" class=" neighborhood-Projects project-vice-emu mountpoint-code">
<div id="nav_menu_holder">
    
        
            



    
    
    
    
    <nav id="breadcrumbs">
        <ul>
            <li itemscope itemtype="http://data-vocabulary.org/Breadcrumb"><a itemprop="url" href="/">Home</a></li>
            <li itemscope itemtype="http://data-vocabulary.org/Breadcrumb"><a itemprop="url" href="/directory">Browse</a></li>
            
            
                
                
            
            
                
            
            
            
                <li itemscope itemtype="http://data-vocabulary.org/Breadcrumb"><a itemprop="url" href="/p/vice-emu/">VICE</a></li>
                
            
            
                <li itemscope itemtype="http://data-vocabulary.org/Breadcrumb">Code</li>
                
            
        </ul>
    </nav>
    
    
    
  
    
      <img src="/p/vice-emu/icon?2013-06-05 13:39:47+00:00" class="project_icon" alt="Project Logo">
    
        <h1 class="project_title">
            <a href="/p/vice-emu/" class="project_link">VICE</a>
        </h1>
    
    
    
    <h2 class="project_summary with-icon">
        Versatile Commodore Emulator
    </h2>
    
    <div class="brought-by with-icon">
        Brought to you by:
        
        
            
                <a href="/u/blackystardust/">blackystardust</a>,
            
            
                <a href="/u/gpz/">gpz</a>
            </div>
    

        
    
</div>
    <div id="top_nav" class="">
        
            
<div id="top_nav_admin">
<ul class="dropdown">
  
    <li class="">
        <a href="/projects/vice-emu/" class="tool-summary-32">
            Summary
        </a>
        
        
    </li>
  
    <li class="">
        <a href="/projects/vice-emu/files/" class="tool-files-32">
            Files
        </a>
        
        
    </li>
  
    <li class="">
        <a href="/projects/vice-emu/reviews" class="tool-reviews-32">
            Reviews
        </a>
        
        
    </li>
  
    <li class="">
        <a href="/projects/vice-emu/support" class="tool-support-32">
            Support
        </a>
        
        
    </li>
  
    <li class="">
        <a href="/p/vice-emu/wiki/" class="tool-wiki-32">
            Wiki
        </a>
        
        
    </li>
  
    <li class="">
        <a href="/p/vice-emu/mailman/" class="tool-mailman-32">
            Mailing Lists
        </a>
        
        
    </li>
  
    <li class="selected">
        <a href="/p/vice-emu/code/" class="tool-svn-32">
            Code
        </a>
        
        
    </li>
  
    <li class="">
        <a href="/p/vice-emu/_list/tickets" class="tool-tickets-32">
            Tickets ▾
        </a>
        
        
            <ul>
                
                    <li class=""><a href="/p/vice-emu/bugs/">Bugs</a></li>
                
                    <li class=""><a href="/p/vice-emu/patches/">Patches</a></li>
                
                    <li class=""><a href="/p/vice-emu/feature-requests/">Feature Requests</a></li>
                
            </ul>
        
    </li>
  
  
</ul>
</div>


        
    </div>
    <div id="content_base">
        
            
                
                    


<div id="sidebar">
  
    <div>&nbsp;</div>
  
    
    
      
      
        
    
      <ul class="sidebarmenu">
      
    
  <li>
      
        <a class="icon" href="/p/vice-emu/code/commit_browser" title="Browse Commits"><i class="fa fa-list"></i>
      
      <span>Browse Commits</span>
      </a>
  </li>
  
      
    
    
      </ul>
      
    
    
</div>
                
                
            
            
                
            
            <div class="grid-20 pad">
                <h2 class="dark title">
<a href="/p/vice-emu/code/31403/">[r31403]</a>:

  
  
    <a href="./../../">testprogs</a> /
    
  
    <a href="./../">CIA</a> /
    
  
    <a href="./">dd0dtest</a> /
    
  
 readme.txt

                    <!-- actions -->
                    <small>
                        

    
    <a class="icon" href="#" id="maximize-content" title="Maximize"><i class="fa fa-expand"></i>&nbsp;Maximize</a>
    <a class="icon" href="#" id="restore-content" title="Restore"><i class="fa fa-compress"></i>&nbsp;Restore</a>
<a class="icon" href="/p/vice-emu/code/31403/log/?path=/testprogs/CIA/dd0dtest/readme.txt" title="History"><i class="fa fa-calendar"></i>&nbsp;History</a>

                    </small>
                    <!-- /actions -->
                </h2>
                
                <div>
                    
  

                    
  
    <p><a href="?format=raw">Download this file</a></p>
    <div class="clip grid-19 codebrowser">
      <h3>
        109 lines (79 with data), 3.8 kB
      </h3>
      
        <table class="codehilitetable"><tbody><tr><td class="linenos"><div class="linenodiv"><pre>  1
  2
  3
  4
  5
  6
  7
  8
  9
 10
 11
 12
 13
 14
 15
 16
 17
 18
 19
 20
 21
 22
 23
 24
 25
 26
 27
 28
 29
 30
 31
 32
 33
 34
 35
 36
 37
 38
 39
 40
 41
 42
 43
 44
 45
 46
 47
 48
 49
 50
 51
 52
 53
 54
 55
 56
 57
 58
 59
 60
 61
 62
 63
 64
 65
 66
 67
 68
 69
 70
 71
 72
 73
 74
 75
 76
 77
 78
 79
 80
 81
 82
 83
 84
 85
 86
 87
 88
 89
 90
 91
 92
 93
 94
 95
 96
 97
 98
 99
100
101
102
103
104
105
106
107
108</pre></div></td><td class="code"><div class="codehilite"><pre><span id="l1" class="code_block">dd0dtest.prg by Wilfred Bos
</span><span id="l2" class="code_block">
</span><span id="l3" class="code_block">- fails on x64 and x64sc (r31051)
</span><span id="l4" class="code_block">
</span><span id="l5" class="code_block">The test program is reading the value of $DD0D before, during and after a NMI
</span><span id="l6" class="code_block">occurs in several ways by doing:
</span><span id="l7" class="code_block">
</span><span id="l8" class="code_block">lda $dd0d
</span><span id="l9" class="code_block">
</span><span id="l10" class="code_block">ldx #$00
</span><span id="l11" class="code_block">lda $dd0d,x
</span><span id="l12" class="code_block">
</span><span id="l13" class="code_block">ldx #$10
</span><span id="l14" class="code_block">lda $dcfd,x
</span><span id="l15" class="code_block">
</span><span id="l16" class="code_block">ldx #$10
</span><span id="l17" class="code_block">lda $ddfd,x
</span><span id="l18" class="code_block">
</span><span id="l19" class="code_block">inc $dd0d
</span><span id="l20" class="code_block">
</span><span id="l21" class="code_block">ldx #$00
</span><span id="l22" class="code_block">inc $dd0d,x
</span><span id="l23" class="code_block">
</span><span id="l24" class="code_block">Some scenarios that are missing from the tests:
</span><span id="l25" class="code_block">
</span><span id="l26" class="code_block">ldx #$10
</span><span id="l27" class="code_block">inc $dcfd,x
</span><span id="l28" class="code_block">
</span><span id="l29" class="code_block">ldx #$10
</span><span id="l30" class="code_block">inc $ddfd,x
</span><span id="l31" class="code_block">
</span><span id="l32" class="code_block">ldy #$00
</span><span id="l33" class="code_block">lda ($fe),y             ; where $fe and $ff points to $dd0d
</span><span id="l34" class="code_block">
</span><span id="l35" class="code_block">ldx #$00
</span><span id="l36" class="code_block">lda ($fe,x)             ; where $fe and $ff points to $dd0d
</span><span id="l37" class="code_block">
</span><span id="l38" class="code_block">and scenarios that are executing code on the CIA registers which can also read
</span><span id="l39" class="code_block">the $dd0d register (e.g. executing a RTS or RTI instruction at $DD0C) are
</span><span id="l40" class="code_block">missing.
</span><span id="l41" class="code_block">
</span><span id="l42" class="code_block">Possible values that are in the output of the tests:
</span><span id="l43" class="code_block">
</span><span id="l44" class="code_block">00 - no NMI occured, no Timer A or B Interrupt
</span><span id="l45" class="code_block">01 - Timer A Interrupt
</span><span id="l46" class="code_block">02 - Timer B Interrupt
</span><span id="l47" class="code_block">81 - NMI occured, Timer A Interrupt
</span><span id="l48" class="code_block">82 - NMI occured, Timer B Interrupt
</span><span id="l49" class="code_block">FF - default init value, writing of DD0D value to output value was not performed
</span><span id="l50" class="code_block">
</span><span id="l51" class="code_block">Tests acktst1 (Test 01) and acktst2 (Test 02) test how long it takes before the
</span><span id="l52" class="code_block">NMI is acknowledged and a new NMI is triggered. There is a different behavior in
</span><span id="l53" class="code_block">the old and new CIA model.
</span><span id="l54" class="code_block">
</span><span id="l55" class="code_block">Tests 03 until 16 are performed in a loop. These tests will test if the NMI is
</span><span id="l56" class="code_block">acknowledged at the right cycle during execution of an instruction that is
</span><span id="l57" class="code_block">reading DD0D. The NMI occurs every 22 cycles.
</span><span id="l58" class="code_block">
</span><span id="l59" class="code_block">Tests dd0dzero0delaytst0 (Test 17) until dd0dzero0delaytst2 (Test 19) test the
</span><span id="l60" class="code_block">behavior of disabling Timer A while enabling Timer B via one instruction and
</span><span id="l61" class="code_block">check the DD0D value. There seems to be difference in the old and new CIA model.
</span><span id="l62" class="code_block">
</span><span id="l63" class="code_block">Some example of how to interpret the test result:
</span><span id="l64" class="code_block">
</span><span id="l65" class="code_block">If e.g. the result is:
</span><span id="l66" class="code_block">
</span><span id="l67" class="code_block">TEST 0C READ  010101010101 FAILED
</span><span id="l68" class="code_block">     EXPECTED 0101FFFF8181
</span><span id="l69" class="code_block">
</span><span id="l70" class="code_block">This means that for test0c the first two output values are correct, namely $01.
</span><span id="l71" class="code_block">The code that writes the value of "sta output+2" and "sta output+3" should not
</span><span id="l72" class="code_block">have been reached because an NMI occurs before these instructions and therefore
</span><span id="l73" class="code_block">the values are not written, hence the value FF as the expected value.
</span><span id="l74" class="code_block">
</span><span id="l75" class="code_block">However the value of DD0D is read as $81 just before the "sta output+2"
</span><span id="l76" class="code_block">instruction and then written during NMI execution as the 5th value of the
</span><span id="l77" class="code_block">output. The NMI was not acknowledge while the last read of DD0D resulted in $81
</span><span id="l78" class="code_block">just before the execution of the code in the NMI routine. Therefore the reading
</span><span id="l79" class="code_block">of DD0D is still $81 in the NMI routine, hence the 6th value is also $81.
</span><span id="l80" class="code_block">
</span><span id="l81" class="code_block">TEST 0C tests the "inc $dd0d,x" for reading DD0D. Note that the execution of this
</span><span id="l82" class="code_block">instruction is like:
</span><span id="l83" class="code_block">
</span><span id="l84" class="code_block">Tn  Address Bus   Data Bus          R/W   Comments
</span><span id="l85" class="code_block">T0  PC            OP CODE           1     Fetch OP CODE
</span><span id="l86" class="code_block">T1  PC + 1        BAL               1     Fetch low order byte of Base Address
</span><span id="l87" class="code_block">T2  PC + 2        BAH               1     Fetch high order byte of Base Address
</span><span id="l88" class="code_block">T3  ADL: BAL + X  Data (Discarded)  1
</span><span id="l89" class="code_block">    ADH: BAH + C
</span><span id="l90" class="code_block">T4  ADL: BAl + X  Data              1     Fetch Data
</span><span id="l91" class="code_block">    ADH: BAH + C
</span><span id="l92" class="code_block">T5  ADH, ADL      Data              0
</span><span id="l93" class="code_block">T6  ADH, ADL      Modified Data     0     Write modified Data back intro memory
</span><span id="l94" class="code_block">T0  PC + 3        OP CODE           1     New Instruction
</span><span id="l95" class="code_block">
</span><span id="l96" class="code_block">Since in this example the test fails, it seems like the "inc $dd0d,x" is not
</span><span id="l97" class="code_block">acknowledging the NMI at cycle T3 but at cycle T4 and therefore the next NMI is
</span><span id="l98" class="code_block">occuring one cycle later which is then acknowledged in the test and therefore the
</span><span id="l99" class="code_block">NMI routine is never executed.
</span><span id="l100" class="code_block">Cycle T3 is discarding the data but it does actually read the data which should
</span><span id="l101" class="code_block">also acknowledge the NMI when reading DD0D.
</span><span id="l102" class="code_block">
</span><span id="l103" class="code_block">TODO
</span><span id="l104" class="code_block">
</span><span id="l105" class="code_block">Still these tests are not exactly written for the new CIA model. The values are
</span><span id="l106" class="code_block">checked for the new model but it will not test the same behavior at the right
</span><span id="l107" class="code_block">cycle. Some tests should run one cycle earlier or later in order to test the
</span><span id="l108" class="code_block">same behavior.
</span></pre></div></td></tr></tbody></table>
      
    </div>
  

                </div>
                
                
            </div>
        
    </div>
</section>
  
<footer id="site-footer">
    <div class="wrapper">
        <nav>
            <h5>SourceForge</h5>
            <a href="/about">About</a>
            <a href="/blog/category/sitestatus/">Site Status</a>
            <a href="http://twitter.com/sfnet_ops">@sfnet_ops</a>
            <a id="allura-notice" href="http://allura.apache.org/">
                <p>Powered by</p>
                <p>Apache Allura™</p>
                <img src="https://a.fsdn.com/allura/nf/1468509372/_ew_/theme/sftheme/images/sftheme/logo-black-svg_g.png" />
            </a>
        </nav>
        <nav>
            <h5>Find and Develop Software</h5>
            <a href="/create/">Create a Project</a>
            <a href="/directory/">Software Directory</a>
            <a href="/top">Top Downloaded Projects</a>
        </nav>
        <nav>
            <h5>Community</h5>
            <a href="/blog/">Blog</a>
            <a href="http://twitter.com/sourceforge">@sourceforge</a>
            <a href="https://library.slashdotmedia.com/">Resources</a>
        </nav>
        <nav>
            <h5>Help</h5>
            <a href="http://p.sf.net/sourceforge/docs">Site Documentation</a>
            <a href="/support">Support Request</a>
        </nav>
    </div>
</footer>
<footer id="site-copyright-footer">
    <div class="wrapper">
        <div id="copyright">
            &copy; 2016 Slashdot Media. All Rights Reserved.<br />
        </div>
        <nav>
            <a href="http://slashdotmedia.com/terms-of-use">Terms</a>
            <a href="http://slashdotmedia.com/privacy-statement/">Privacy</a>
            <span id='teconsent'></span>
            <a href="http://slashdotmedia.com/opt-out-choices">Opt Out Choices</a>
            <a href="http://slashdotmedia.com">Advertise</a>
        </nav>
    </div>
</footer>


    
<div id="newsletter-floating" class="goth-form">
    <h2>Get latest updates about Open Source Projects, Conferences and News.</h2>
    <p>Sign up for the SourceForge newsletter:</p>
    
    <form action="/user/newsletters/subscribe" method="post">
    <div class="form">
        <input type="email" name="XfL_q_r9jXqlN4Ukjo9hEoeh2cFQ"  placeholder="email@address.com" value="">
    <input type="submit" value="Subscribe" class="bt">
    </div>
    <div class="fielderror"></div>

    <p class="details">
    <span class="fielderror"></span>
    <label>
        <input type="checkbox" required name="Xfrno8aVqx0jH-aRrqd8-vHfhB7Y" value="true" >I agree to receive quotes, newsletters and other information from sourceforge.net and its partners regarding IT services and products. I understand that I can withdraw my consent at any time. Please refer to our <a href="https://slashdotmedia.com/privacy-statement/">Privacy Policy</a> or <a href="/support">Contact Us</a> for more details
        <input type="hidden" name="XdLTi-rJ89l9fAqqDV25T6-qz6Ds" value="true">
    </label>
    </p>

    <input type="hidden" name="source" value="floating">
    <input type="hidden" name="Xdbno6rh720VvD7aCXO0xAxquvWQ" value="DE">
    <input type="hidden" name="Xarno6rh720VvD7aCXEXXEeJMG5s" value="ip_address">
    <input type="hidden" name="XcrTi6KVjzEhECauVzo_Z8wf74jw" value="sitewide">
    <input type="hidden" name="XcrTi6KVjzEhECauVzo_Z8wf74jw" value="research">
    <input type="hidden" name="XcrTi6KVjzEhECauVzo_Z8wf74jw" value="events">
    <input type="hidden" name="XcrTi6KVjzEhECauVzo_Z8wf74jw" value="thirdparty">
    <input type="hidden" name="XcrTi6KVjzEhECauVzo_Z8wf74jw" value="all">
    <input type="hidden" name="XerHq6iGa1LGgqUMk2cXbNp-UFn4" value="">
    
  <input type="hidden" name="_visit_cookie" value="57896332c1c78b77fae76d31"/>
    <input id="w-5af" name="timestamp" type="hidden" value="1468650823">
    <input id="w-5b0" name="spinner" type="hidden" value="XedqHn9YPqTwwbNnmORqkfpc-eP4">
    <p class="XZYLidHxwVpPKiyTmGMPlTNw">
    <label for="XfrLo8bN2mQzH-aRrqd8-vHfhB7Y">You seem to have CSS turned off.
        Please don't fill out this field.</label><br>
    <input id="XfrLo8bN2mQzH-aRrqd8-vHfhB7Y" name="Xf7Lo8bN2mculEVR2_IBmnkhBMPY" type="text"><br></p>
    <p class="XZYLidHxwVpPKiyTmGMPlTNw">
    <label for="XfrLo8bN2mQ3H-aRrqd8-vHfhB7Y">You seem to have CSS turned off.
        Please don't fill out this field.</label><br>
    <input id="XfrLo8bN2mQ3H-aRrqd8-vHfhB7Y" name="Xf7Lo8bN2mMulEVR2_IBmnkhBMPY" type="text"><br></p>
</form>

    <a id="btn-float-close">No, thanks</a>
</div>


<div id="messages">
    
</div>


    <!-- ew:body_js -->


    <script type="text/javascript" src="https://a.fsdn.com/allura/nf/1468509372/_ew_/_slim/js?href=allura%2Fjs%2Fjquery.notify.js%3Ballura%2Fjs%2Fjquery.tooltipster.js%3Ballura%2Fjs%2Fmodernizr.js%3Ballura%2Fjs%2Fsylvester.js%3Ballura%2Fjs%2Ftwemoji.min.js%3Ballura%2Fjs%2Fpb.transformie.min.js%3Ballura%2Fjs%2Fallura-base.js%3Ballura%2Fjs%2Fadmin_modal.js%3Bjs%2Fjquery.lightbox_me.js%3Btheme%2Fsftheme%2Fjs%2Fsftheme%2Fshared.js%3Ballura%2Fjs%2Fmaximize-content.js"></script>

    
<!-- /ew:body_js -->



    <!-- ew:body_js_tail -->


    
<!-- /ew:body_js_tail -->




<script type="text/javascript">(function() {
  $('#access_urls .btn').click(function(evt){
    evt.preventDefault();
    var parent = $(this).parents('.btn-bar');
    $(parent).find('input').val($(this).attr('data-url'));
    $(parent).find('span').text($(this).attr('title')+' access');
    $(this).parent().children('.btn').removeClass('active');
    $(this).addClass('active');
  });
  $('#access_urls .btn').first().click();

  
  var repo_status = document.getElementById('repo_status');
  // The repo_status div will only be present if repo.status != 'ready'
  if (repo_status) {
    $('.spinner').show()
    var delay = 500;
    function check_status() {
        $.get('/p/vice-emu/code/status', function(data) {
            if (data.status === 'ready') {
                $('.spinner').hide()
                $('#repo_status h2').html('Repo status: ready. <a href=".">Click here to refresh this page.</a>');
            }
            else {
                $('#repo_status h2 span').html(data.status);
                if (delay < 60000){
                    delay = delay * 2;
                }
                window.setTimeout(check_status, delay);
            }
        });
    }
    var status_checker = window.setTimeout(check_status, delay);
    
  }
}());
</script>

<script type="text/javascript">(function() {
  $(window).bind('hashchange', function(e) {
    var hash = window.location.hash.substring(1);
	if ('originalEvent' in e && 'oldURL' in e.originalEvent) {
      $('#' + e.originalEvent.oldURL.split('#')[1]).css('background-color', 'transparent');
	}
    if (hash !== '' && hash.substring(0, 1) === 'l' && !isNaN(hash.substring(1))) {
      $('#' + hash).css('background-color', '#ffff99');
    }
  }).trigger('hashchange');

  var clicks = 0;
  $('.code_block').each(function(index, element) {
    $(element).bind('click', function() {
      // Trick to ignore double and triple clicks
      clicks++;
      if (clicks == 1) {
        setTimeout(function() {
          if (clicks == 1) {
            var hash = window.location.hash.substring(1);
            if (hash !== '' && hash.substring(0, 1) === 'l' && !isNaN(hash.substring(1))) {
              $('#' + hash).css('background-color', 'transparent');
            }
            $(element).css('background-color', '#ffff99');
            window.location.href = '#' + $(element).attr('id');
          };
          clicks = 0;
        }, 500);
      };
    });
  });
}());
</script>


    


    <!-- Google Code for Remarketing tag -->
    <!-- Remarketing tags may not be associated with personally identifiable information or placed on pages related to sensitive categories. For instructions on adding this tag and more information on the above requirements, read the setup guide: google.com/ads/remarketingsetup -->
    <script type="text/javascript">
        /* <![CDATA[ */
        var google_conversion_id = 1002083962;
        var google_conversion_label = "G_uGCOaBlAQQ-qzq3QM";
        var google_custom_params = window.google_tag_params;
        var google_remarketing_only = true;
        /* ]]> */
    </script>
    <script type="text/javascript" src="//www.googleadservices.com/pagead/conversion.js"> </script>
    <script type="text/javascript" src='//consent-st.truste.com/get?name=notice.js&domain=slashdot.org&c=teconsent&text=true'></script>
    <noscript>
      <div style="display:inline;">
        <img height="1" width="1" style="border-style:none;" alt="" src="//googleads.g.doubleclick.net/pagead/viewthroughconversion/1002083962/?value=0&amp;label=G_uGCOaBlAQQ-qzq3QM&amp;guid=ON&amp;script=0"/>
      </div>
    </noscript>

     
      

<script>
    $(document).ready(function () {
        $(".tooltip").tooltipster({
            animation: 'fade',
            delay: 200,
            theme: 'tooltipster-light',
            trigger: 'hover',
            position: 'right',
            iconCloning: false,
            maxWidth: 300
        }).focus(function () {
            $(this).tooltipster('show');
        }).blur(function () {
            $(this).tooltipster('hide');
        });
    });
</script>
</body>
</html>