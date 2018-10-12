package uk.gov.justice.services.event.sourcing.subscription.manager;

import static java.util.Arrays.asList;
import static java.util.Collections.EMPTY_LIST;
import static org.codehaus.groovy.runtime.InvokerHelper.asList;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.subscription.SubscriptionManager;
import uk.gov.justice.subscription.domain.subscriptiondescriptor.Subscription;
import uk.gov.justice.subscription.domain.subscriptiondescriptor.SubscriptionsDescriptor;
import uk.gov.justice.subscription.registry.SubscriptionsDescriptorsRegistry;

import java.util.List;

import javax.enterprise.inject.Instance;

import com.google.common.collect.Sets;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SubscriptionManagerStartUpTest {

    @Mock(answer = RETURNS_DEEP_STUBS)
    private Instance<SubscriptionManager> subscriptionManagerBeans;

    @Mock
    private SubscriptionsDescriptorsRegistry descriptorRegistry;
    @Mock
    private SubscriptionsDescriptor descriptor;

    @InjectMocks
    private SubscriptionManagerStartUp subscriptionManagerStartUp;

    @SuppressWarnings("unchecked")
    @Test
    public void shouldStartSubscription() {

        final SubscriptionManager subscriptionManager = mock(SubscriptionManager.class);

        when(descriptorRegistry.subscriptionsDescriptors()).thenReturn(Sets.newHashSet(descriptor));
        when(descriptor.getSubscriptions()).thenReturn(asList(mock(Subscription.class)));
        when(subscriptionManagerBeans.select(any(SubscriptionNameQualifier.class)).get()).thenReturn(subscriptionManager);

        subscriptionManagerStartUp.start();

        verify(subscriptionManager).startSubscription();
    }

    @Test
    public void shouldStartAllAvailableSubscription() {

        List<SubscriptionManager> subscriptionManagers = asList(mock(SubscriptionManager.class), mock(SubscriptionManager.class));

        when(descriptorRegistry.subscriptionsDescriptors()).thenReturn(Sets.newHashSet(descriptor));
        when(descriptor.getSubscriptions()).thenReturn(asList(mock(Subscription.class), mock(Subscription.class)));

        when(subscriptionManagerBeans.select(any(SubscriptionNameQualifier.class)).get())
                .thenReturn(subscriptionManagers.get(0))
                .thenReturn(subscriptionManagers.get(1));

        subscriptionManagerStartUp.start();

        verify(subscriptionManagers.get(0)).startSubscription();
        verify(subscriptionManagers.get(1)).startSubscription();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldDoNothingWhenNoSubscriptionsExist() {

        when(descriptorRegistry.subscriptionsDescriptors()).thenReturn(Sets.newHashSet(descriptor));
        when(descriptor.getSubscriptions()).thenReturn(EMPTY_LIST);

        subscriptionManagerStartUp.start();

        verifyZeroInteractions(subscriptionManagerBeans);
    }


}
