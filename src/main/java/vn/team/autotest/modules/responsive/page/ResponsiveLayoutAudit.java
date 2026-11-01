
package vn.team.autotest.modules.responsive.page;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

/**
 * Audit layout không thay đổi DOM, không click, không auto-PASS.
 * Kiểm tra trang dọc / ngang; phân biệt nút ở ngoài vùng nhìn thấy vì scroll
 * với nút thực sự bị che trên màn hình.
 */
public final class ResponsiveLayoutAudit {
    private final WebDriver driver;
    private final JavascriptExecutor js;

    public ResponsiveLayoutAudit(WebDriver driver) {
        this.driver = driver;
        this.js = (JavascriptExecutor) driver;
    }

    @SuppressWarnings("unchecked")
    public String inspect(String checkpoint) {
        Map<String, Object> result = (Map<String, Object>) js.executeScript("""
          const vw=document.documentElement.clientWidth, vh=window.innerHeight;
          const pageWidth=Math.max(document.documentElement.scrollWidth,document.body?.scrollWidth||0);
          const errors=[], notes=[];
          const name=e=>!e?'document':e.tagName.toLowerCase()
            +(e.id?'#'+e.id:'')
            +(typeof e.className==='string'&&e.className.trim()?'.'+e.className.trim().replace(/\\s+/g,'.').slice(0,65):'');
          const issue=(kind,e,detail)=>{if(errors.length<30)errors.push(kind+' | '+name(e)+' | '+detail)};
          const note=(kind,e,detail)=>{if(notes.length<20)notes.push(kind+' | '+name(e)+' | '+detail)};
          const r=e=>e.getBoundingClientRect(),css=e=>getComputedStyle(e);
          const visible=e=>{
            const b=r(e),s=css(e);
            if(!e.getClientRects().length||b.width<2||b.height<2)return false;
            if(s.display==='none'||s.visibility!=='visible'||+s.opacity===0)return false;
            if(e.closest('[hidden],[inert],[aria-hidden="true"]'))return false;
            for(let p=e.parentElement;p;p=p.parentElement){
              const t=css(p);
              if(t.display==='none'||t.visibility==='hidden'||+t.opacity===0)return false;
            }
            return true;
          };
          const offscreenPanel=e=>{
            const p=e.closest('#sidebar,#mobile-sidebar,aside.sidebar,aside[class*="sidebar"],#mobileMenu,.mobile-menu');
            if(!p)return false;
            const b=r(p);
            return b.right<=3||b.left>=vw-3;
          };
          const clippedByHorizontalScroll=e=>{
            for(let p=e.parentElement;p;p=p.parentElement){
              const s=css(p);
              if(/auto|scroll/.test(s.overflowX)&&p.scrollWidth>p.clientWidth+3)return true;
            }
            return false;
          };
          const samples=[[.5,.5],[.25,.5],[.75,.5],[.5,.25],[.5,.75]];
          const intersection=(a,b)=>Math.max(0,Math.min(a.right,b.right)-Math.max(a.left,b.left))
            *Math.max(0,Math.min(a.bottom,b.bottom)-Math.max(a.top,b.top));
          const viewport={left:0,top:0,right:vw,bottom:vh};
          const fractionInView=e=>{
            const b=r(e);return intersection(b,viewport)/Math.max(1,b.width*b.height);
          };
          const controls=[...new Set(document.querySelectorAll(
            'button,a[href],input,select,textarea,[role="button"]'))]
            .filter(e=>visible(e)&&!e.disabled&&!offscreenPanel(e)
              &&!e.closest('#pww-test-marker,[data-pww-test-focus]'));

          // Chỉ cảnh báo phần tử có scroll nội bộ; không bỏ qua tràn toàn trang.
          if(pageWidth>vw+3)issue('PAGE_OVERFLOW',null,'scrollWidth='+pageWidth+' viewport='+vw);
          if(vw<240)issue('VIEWPORT_TOO_NARROW',null,'clientWidth='+vw);
          // Không bắt buộc <= 500: landscape 896x414 là viewport hợp lệ.

          const elements=[...new Set(document.querySelectorAll(
            'header,main,footer,main *,header *,footer *,#categoryGrid *,[role="dialog"] *,[role="alertdialog"] *'))]
            .slice(0,4000).filter(e=>visible(e)&&!offscreenPanel(e)
              &&!e.closest('#pww-test-marker,[data-pww-test-focus]'));
          for(const e of elements){
            const b=r(e),s=css(e);
            if(b.right>vw+4||b.left< -4){
              if(!clippedByHorizontalScroll(e)&&fractionInView(e)>.05){
                issue('ELEMENT_OUTSIDE',e,'left='+Math.round(b.left)+',right='+Math.round(b.right)+',vw='+vw);
              }
            }
            if(e.matches('h1,h2,h3,.cat-title,.category-title') &&
               e.scrollWidth>e.clientWidth+3 &&
               (['hidden','clip'].includes(s.overflowX)||s.textOverflow==='ellipsis'||s.whiteSpace==='nowrap')){
               issue('TEXT_CLIPPED',e,'scrollWidth='+e.scrollWidth+',clientWidth='+e.clientWidth);
            }
          }
          const modalSelectors='#startModal,.modal-overlay,.modal.show,.swal2-container,[role="dialog"],'
            +'[role="alertdialog"],[aria-modal="true"],#exitConfirmModal,#confirmExitModal,'
            +'.confirm-dialog,.confirmation-modal';
          const modal=[...document.querySelectorAll(modalSelectors)].filter(e=>{
            if(!visible(e))return false;
            const b=r(e);return intersection(b,viewport)>0 && css(e).pointerEvents!=='none';
          }).sort((a,b)=>b.querySelectorAll('button').length-a.querySelectorAll('button').length)[0]||null;
          if(modal){
            const b=r(modal);
            if(b.left < -4||b.right>vw+4)
              issue('DIALOG_OVERFLOW',modal,'left='+Math.round(b.left)+',right='+Math.round(b.right));
          }
          const onStage=controls.filter(e=>!modal||modal.contains(e));
          // Bắt chồng đè thật giữa nút độc lập khi cùng xuất hiện trong viewport.
          // Không so sánh node cha / con, không xem overlay đang mở là lỗi.
          for(let i=0;i<onStage.length;i++){
            const a=onStage[i],ba=r(a);
            if(fractionInView(a)<.70)continue;
            for(let j=i+1;j<onStage.length;j++){
              const b=onStage[j],bb=r(b);
              if(a.contains(b)||b.contains(a)||fractionInView(b)<.70)continue;
              const ratio=intersection(ba,bb)/Math.max(1,Math.min(ba.width*ba.height,bb.width*bb.height));
              if(ratio>.35){
                const ca=a.closest('header'),cb=b.closest('header');
                // Header menu có nút chồng back => cần xác minh dù không cùng parent.
                if(ca&&cb || a.parentElement===b.parentElement)
                   issue('CONTROL_OVERLAP',a,'with '+name(b)+' '+Math.round(ratio*100)+'%');
              }
            }
          }

          // Lấy mẫu vùng click thực tế tại 5 điểm; không tự click.
          const hitTest=e=>{
            const b=r(e),blockers=[];
            for(const [fx,fy] of samples){
              const x=b.left+fx*b.width,y=b.top+fy*b.height;
              if(x<2||x>vw-2||y<2||y>vh-2)continue;
              const top=document.elementFromPoint(x,y);
              if(!top)continue;
              if(top===e||e.contains(top))return {ok:true,blockers:[]};
              blockers.push(top);
            }
            return {ok:false,blockers};
          };

          // Kiểm tra lại nút bị che sau khi cuộn vào vị trí thao tác hợp lệ.
          // Khôi phục vị trí cuộn ban đầu để không thay đổi trạng thái TC.
          const probeReachable=e=>{
            const oldX=window.scrollX,oldY=window.scrollY;
            let byTop=false,byCenter=false;
            try {
              if(oldY>2){
                window.scrollTo({left:0,top:0,behavior:'instant'});
                byTop=hitTest(e).ok;
              }
              if(!byTop){
                e.scrollIntoView({block:'center',inline:'nearest',behavior:'instant'});
                byCenter=hitTest(e).ok;
              }
            } finally {
              window.scrollTo({left:oldX,top:oldY,behavior:'instant'});
            }
            return {reachable:byTop||byCenter,byTop,byCenter};
          };

          for(const e of onStage){
            const b=r(e),s=css(e);
            if(fractionInView(e)<.80)continue;
            if(b.width<12||b.height<12)continue;
            if(b.left<0||b.right>vw||b.top<0||b.bottom>vh)continue;
            const h=hitTest(e);
            if(h.ok||h.blockers.length===0)continue;
            const blockers=h.blockers;
            const blockersText=[...new Set(blockers.map(name))].join(',');

            // Nếu có thể truy cập bằng cuộn trang bình thường,
            // đây là hiện tượng che tạm thời thay vì lỗi cố định.
            if(!['fixed','sticky'].includes(s.position)){
              const probe=probeReachable(e);
              if(probe.reachable){
                note('RECOVERABLE_SCROLL_OCCLUSION',e,
                  'originalScroll='+Math.round(window.scrollY)
                  +'; availableAfter='+(probe.byTop?'scrollTop':'scrollIntoView')
                  +'; blockedAtCheckpointBy='+blockersText);
                continue;
              }
            }

            issue('CONTROL_COVERED',e,
              'blocked by '+blockersText
              +'; scrollY='+Math.round(window.scrollY)
              +'; rect=['+Math.round(b.left)+','+Math.round(b.top)
              +','+Math.round(b.right)+','+Math.round(b.bottom)+']');
          }

          return {viewport:vw+'x'+vh,scrollWidth:pageWidth,
              errors:[...new Set(errors)].slice(0,20),
              notes:[...new Set(notes)].slice(0,10)};
          """);

        List<String> errors = new ArrayList<>();
        List<String> notes = new ArrayList<>();

        for (Object s : (List<?>) result.get("errors")) {
            errors.add(String.valueOf(s));
        }

        for (Object s : (List<?>) result.get("notes")) {
            notes.add(String.valueOf(s));
        }

        String desc = "[RESPONSIVE] " + checkpoint
            + " | " + result.get("viewport")
            + " | scrollWidth=" + result.get("scrollWidth")
            + " | errors=" + errors.size()
            + " | notes=" + notes.size();

        System.out.println(desc);

        for (String s : notes) {
            System.out.println("[RESPONSIVE NOTE] " + s);
        }

        if (!errors.isEmpty()) {
            throw new AssertionError(
                "RESPONSIVE FAIL: " + checkpoint
                + " | URL=" + driver.getCurrentUrl()
                + "\n- " + String.join("\n- ", errors)
            );
        }

        return desc;
    }
}
