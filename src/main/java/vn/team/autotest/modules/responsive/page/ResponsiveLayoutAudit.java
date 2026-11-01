package vn.team.autotest.modules.responsive.page;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

/**
 * Responsive audit: không thay đổi DOM/CSS, chỉ đọc kích thước & hit testing.
 * Kiểm tra tràn ngang, control ngoài khung, bị che, chồng lên nhau và chữ cắt.
 * Các trạng thái menu/modal đang mở được xem là overlay hợp lệ.
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
        Map<String, Object> data = (Map<String, Object>) js.executeScript("""
            const vw=document.documentElement.clientWidth;
            const vh=window.innerHeight;
            const scrollWidth=Math.max(document.documentElement.scrollWidth,
                document.body?.scrollWidth||0);
            const issues=[];
            const ident=e=>!e?'document':(e.tagName.toLowerCase() +
                (e.id?'#'+e.id:'') +
                (typeof e.className==='string' ? '.'+e.className.trim().replace(/\\s+/g,'.').slice(0,60) : ''));
            const add=(type,e,reason)=>{
                if(issues.length<30) issues.push(type+' | '+ident(e)+' | '+reason);
            };
            const hidden=e=>{
                const s=getComputedStyle(e),r=e.getBoundingClientRect();
                if(!e.getClientRects().length || r.width<2 || r.height<2) return true;
                if(s.display==='none'||s.visibility==='hidden'||Number(s.opacity)===0) return true;
                if(e.closest('[aria-hidden="true"],[inert]'))return true;
                if(s.backfaceVisibility==='hidden' && s.transform!=='none'){
                    const matrix=new DOMMatrixReadOnly(s.transform);
                    if(matrix.m33<0)return true;
                }
                return false;
            };
            const insideScroller=e=>{
                let p=e.parentElement;
                while(p&&p!==document.body){
                    const s=getComputedStyle(p);
                    if(['auto','scroll'].includes(s.overflowX) && p.scrollWidth>p.clientWidth+3) return true;
                    p=p.parentElement;
                }
                return false;
            };
            const intentionalOffscreen=e=>{
                if(e.closest('#sidebar,#mobile-sidebar,aside.sidebar,aside[class*="sidebar"],'
                    +'[data-sidebar-backdrop],#mobile-menu-overlay,.mobile-menu-overlay')) {
                    const r=e.getBoundingClientRect();
                    if(r.right<=8 || r.left>=vw-8) return true;
                }
                return false;
            };
            const rect=e=>e.getBoundingClientRect();
            if(vw>500) add('VIEWPORT',null,'mobile clientWidth='+vw);
            if(scrollWidth>vw+3) add('PAGE_OVERFLOW',null,'scrollWidth='+scrollWidth+' viewport='+vw);
            const selector='header,header a,header button,main,main *,footer,footer *,form,form *,'+
                '#categoryGrid,#categoryGrid *,[role="dialog"],[role="dialog"] *,'+
                '[role="alertdialog"],[role="alertdialog"] *,h1,h2,h3';
            const els=[...new Set(document.querySelectorAll(selector))].slice(0,3500)
                .filter(e=>!hidden(e)&&!e.closest('#pww-test-marker,[data-pww-test-focus]')
                    && !intentionalOffscreen(e));
            for(const e of els){
                const r=rect(e),s=getComputedStyle(e);
                if(!insideScroller(e) && (r.right>vw+4||r.left < -4)){
                    if(!(['fixed','sticky'].includes(s.position) && (r.right<=0||r.left>=vw)))
                        add('ELEMENT_OUTSIDE',e,'left='+Math.round(r.left)+' right='+Math.round(r.right)+' vw='+vw);
                }
                if(e.matches('h1,h2,h3,.cat-title,.category-title,#categoryGrid .cat-card h3')){
                    if(e.scrollWidth>e.clientWidth+3 &&
                        (['hidden','clip'].includes(s.overflowX)||s.textOverflow==='ellipsis'||s.whiteSpace==='nowrap'))
                        add('TEXT_CLIPPED',e,'scrollWidth='+e.scrollWidth+' clientWidth='+e.clientWidth+
                            ' text='+((e.textContent||'').trim().slice(0,55)));
                }
                if(e.matches('input,select,textarea,button') && r.width<24 && r.height<24 &&
                    !e.closest('[role="dialog"],[role="alertdialog"]')){
                    add('TOO_SMALL',e,Math.round(r.width)+'x'+Math.round(r.height));
                }
            }
            // Kiểm tra đè giữa các control ngang hàng, tránh so sánh phần tử lồng nhau.
            const controlSelector='main input,main select,main textarea,main button,main a[href],'
                +'header button,header a[href],footer button,[role="dialog"] button,'
                +'[role="alertdialog"] button';
            const ctrls=[...document.querySelectorAll(controlSelector)]
                .filter(e=>!hidden(e)&&!e.disabled&&!intentionalOffscreen(e)
                    &&!e.closest('#pww-test-marker'));
            for(let i=0;i<ctrls.length;i++){
                const a=ctrls[i],ra=rect(a);
                for(let j=i+1;j<ctrls.length;j++){
                    const b=ctrls[j];
                    if(a.contains(b)||b.contains(a)||a.parentElement!==b.parentElement)continue;
                    const rb=rect(b);
                    const w=Math.max(0,Math.min(ra.right,rb.right)-Math.max(ra.left,rb.left));
                    const h=Math.max(0,Math.min(ra.bottom,rb.bottom)-Math.max(ra.top,rb.top));
                    const ratio=w*h/Math.max(1,Math.min(ra.width*ra.height,rb.width*rb.height));
                    if(ratio>0.30) add('CONTROL_OVERLAP',a,'with '+ident(b)+' '+Math.round(ratio*100)+'%');
                }
            }
            // Hit-test để phát hiện control bị lớp vô hình/element khác che chắn.
            // Chỉ xét control nằm trong viewport, không xét vùng ngoài do chưa cuộn.
            // Popup/sidebar đang mở được phép che nội dung bên dưới.
            const activeOverlay=[...document.querySelectorAll(
                '[role="dialog"],[role="alertdialog"],#sidebar,#mobile-sidebar,aside.sidebar')]
                .some(e=>!hidden(e)&&(()=>{const r=rect(e);return r.left<vw-8&&r.right>8;})());
            if(!activeOverlay){
                for(const e of ctrls){
                    const r=rect(e);
                    if(r.bottom<=3||r.top>=vh-3||r.left<0||r.right>vw)continue;
                    const x=Math.max(2,Math.min(vw-2,(r.left+r.right)/2));
                    const y=Math.max(2,Math.min(vh-2,(r.top+r.bottom)/2));
                    const top=document.elementFromPoint(x,y);
                    if(top&&!e.contains(top)&&!top.contains(e)){
                        const overlay=top.closest('#pww-test-marker,[data-pww-test-focus]');
                        if(!overlay) add('CONTROL_COVERED',e,'center blocked by '+ident(top));
                    }
                }
            }
            return {viewport:vw+'x'+vh,scrollWidth:scrollWidth,
                issues:[...new Set(issues)].slice(0,20)};
            """);
        List<String> issues = new ArrayList<>();
        for (Object issue : (List<?>) data.get("issues")) issues.add(String.valueOf(issue));
        String details = "[RESPONSIVE] " + checkpoint + " | " + data.get("viewport")
            + " | scrollWidth=" + data.get("scrollWidth") + " | issues=" + issues.size();
        System.out.println(details);
        if (!issues.isEmpty()) {
            throw new AssertionError("RESPONSIVE FAIL: " + checkpoint + " | URL="
                + driver.getCurrentUrl() + "\n- " + String.join("\n- ", issues));
        }
        return details;
    }
}